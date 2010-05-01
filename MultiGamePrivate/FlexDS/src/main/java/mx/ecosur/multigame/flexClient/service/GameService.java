/*
* Copyright (C) 2010 ECOSUR, Andrew Waterman and Max Pimm
* 
* Licensed under the Academic Free License v. 3.2. 
* http://www.opensource.org/licenses/afl-3.0.php
*/

package mx.ecosur.multigame.flexClient.service;

import java.util.*;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import mx.ecosur.multigame.ejb.interfaces.RegistrarRemote;
import mx.ecosur.multigame.ejb.interfaces.SharedBoardRemote;
import mx.ecosur.multigame.enums.SuggestionStatus;
import mx.ecosur.multigame.exception.InvalidMoveException;
import mx.ecosur.multigame.exception.InvalidRegistrationException;
import mx.ecosur.multigame.exception.InvalidSuggestionException;
import mx.ecosur.multigame.flexClient.exception.GameException;

import mx.ecosur.multigame.impl.Color;
import mx.ecosur.multigame.impl.entity.manantiales.ManantialesMove;
import mx.ecosur.multigame.impl.entity.manantiales.PuzzleSuggestion;
import mx.ecosur.multigame.impl.entity.manantiales.SimpleAgent;
import mx.ecosur.multigame.impl.enums.manantiales.AgentType;
import mx.ecosur.multigame.impl.enums.manantiales.Mode;
import mx.ecosur.multigame.impl.model.GameGrid;
import mx.ecosur.multigame.impl.model.GridGame;
import mx.ecosur.multigame.impl.model.GridMove;
import mx.ecosur.multigame.impl.model.GridPlayer;
import mx.ecosur.multigame.impl.model.GridRegistrant;
import mx.ecosur.multigame.impl.entity.gente.GenteGame;
import mx.ecosur.multigame.impl.entity.gente.GenteStrategyAgent;
import mx.ecosur.multigame.impl.entity.manantiales.ManantialesGame;
import mx.ecosur.multigame.impl.enums.gente.*;

import mx.ecosur.multigame.model.*;


import flex.messaging.FlexContext;
import flex.messaging.FlexSession;

public class GameService {  

    private SharedBoardRemote getSharedBoard() {
        FlexSession session = FlexContext.getFlexSession();
        SharedBoardRemote sharedBoard = (SharedBoardRemote) session.getAttribute("sharedBoard");
        if (sharedBoard == null) {
            try {
                InitialContext ic = new InitialContext();
                sharedBoard = (SharedBoardRemote) ic.lookup(SharedBoardRemote.class.getName());
                session.setAttribute("sharedBoard", sharedBoard);
            } catch (NamingException e) {
                e.printStackTrace();
                throw new GameException(e);
            }
        }
        return sharedBoard;
}

    private RegistrarRemote getRegistrar() {
        FlexSession session = FlexContext.getFlexSession();
        RegistrarRemote registrar = (RegistrarRemote) session.getAttribute("registrar");
        if (registrar == null) {
            try {
                InitialContext ic = new InitialContext();
                registrar = (RegistrarRemote) ic.lookup(RegistrarRemote.class.getName());
                session.setAttribute("registrar", registrar);
            } catch (NamingException e) {
                e.printStackTrace();
                throw new GameException(e);
            }
        }
        return registrar;
    }

    public GridRegistrant login (String name) {

        GridRegistrant gr = new GridRegistrant (name);
        RegistrarRemote registrar = this.getRegistrar();
        Registrant registrant = registrar.register(new Registrant (gr));
        return (GridRegistrant) registrant.getImplementation();
    }    

    public ServiceGameEvent startNewGame(GridRegistrant player, Color preferedColor, String gameTypeStr)
    {
        ServiceGameEvent ret = null;
        try {
            RegistrarRemote registrar = getRegistrar();
            GridGame game = null;
            GameType type = GameType.valueOf(gameTypeStr);

            switch (type) {
                case GENTE:
                    game = new GenteGame();
                    break;
                case MANANTIALES:
                    game = new ManantialesGame();
                    break;
            }

            Game model = new Game (game);
            model = registrar.registerPlayer(model, new Registrant(player));
            for (GamePlayer impl : model.listPlayers()) {
                GridPlayer gridPlayer = (GridPlayer) impl.getImplementation();
                if (gridPlayer.getRegistrant().equals(player)) {
                    ret = new ServiceGameEvent ((GridGame) model.getImplementation(), gridPlayer);
                    break;
                }
            }

        } catch (InvalidRegistrationException e) {
            e.printStackTrace();
            throw new GameException(e);
        } catch (RuntimeException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } catch (NoClassDefFoundError e) {
            e.printStackTrace();
        }

        return ret;
    }

    public ServiceGameEvent startNewGame(GridRegistrant player, Color preferedColor, String gameTypeStr, String mode) {
        ServiceGameEvent ret = startNewGame (player, preferedColor, gameTypeStr);
        GridGame game = ret.getGame();
        if (game instanceof ManantialesGame) {
            ManantialesGame m = (ManantialesGame) game;
            if (mode != null && mode.length() > 0) {
                Mode modality = Mode.valueOf(mode);
                m.setMode(modality.decrement());
            } 
            ret.setGame(game);
        }

        /* Publish modified game */
        SharedBoardRemote sharedBoard = getSharedBoard();
        sharedBoard.shareGame(game);

        return ret;
    }

    public ServiceGameEvent joinPendingGame (GridGame game, GridRegistrant registrant,
                    Color preferredColor)
    {
        ServiceGameEvent ret = null;
        RegistrarRemote registrar = getRegistrar();
        try {
            Game model = registrar.registerPlayer (new Game(game),
                                new Registrant(registrant));
            for (GamePlayer gp : model.listPlayers()) {
                GridPlayer player = (GridPlayer) gp.getImplementation();
                if (player.getRegistrant().equals(registrant)) {
                    ret = new ServiceGameEvent ((GridGame) model.getImplementation(), player);
                    break;
                }
            }

        } catch (InvalidRegistrationException e) {
                e.printStackTrace();
                throw new GameException (e);
        }

        return ret;
    }

    public boolean quitGame (GridGame game, GridPlayer player) {
        boolean ret = false;
        try {
            RegistrarRemote registrar = getRegistrar();
            registrar.unregister (new Game (game), new GamePlayer (player));
            ret = true;
        } catch (InvalidRegistrationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return ret;
    }

    /*
     * Starts a new game with a given player and the number of selected AI robots.
     */
    public ServiceGameEvent startNewGameWithAI (GridRegistrant registrant, Color preferredColor,
        String gameTypeStr, String [] strategies)
    {
        ServiceGameEvent ret = null;
        try {
            RegistrarRemote registrar = getRegistrar();
            Game model = null;
            GameType gameType = GameType.valueOf(gameTypeStr);
            if (gameType.equals(GameType.GENTE)) {
                GridGame game = new GenteGame ();
                model = registrar.registerPlayer(new Game(game), new Registrant (registrant));
                for (int i = 0; i < strategies.length; i++) {
                    if (strategies [ i ].equals("HUMAN"))
                        continue;
                    GenteStrategy strategy = GenteStrategy.valueOf(
                        strategies [ i ]);
                    GridRegistrant robot = new GridRegistrant (
                        strategy.name() + "-" + (i + 1));
                    GenteStrategyAgent agent = new GenteStrategyAgent (robot, Color.UNKNOWN, strategy);
                    model = registrar.registerAgent (model, new Agent (agent));
                }
            } else if (gameType.equals(GameType.MANANTIALES)) {
                GridGame game = new ManantialesGame ();
                model = registrar.registerPlayer(new Game(game), new Registrant (registrant));
                for (int i = 0; i < strategies.length; i++) {
                    GridRegistrant robot = new GridRegistrant (strategies [ i ] + "-" + (i + 1));
                    AgentType type = AgentType.valueOf(strategies [ i ]);
                    SimpleAgent agent = new SimpleAgent(robot, Color.UNKNOWN, type);
                    model = registrar.registerAgent (model, new Agent (agent));
                }
            }

            if (model != null) {
                for (GamePlayer player : model.listPlayers()) {
                    GridPlayer gp = (GridPlayer) player.getImplementation();
                    if (gp.getRegistrant().equals(registrant)) {
                        ret = new ServiceGameEvent ((GridGame) model.getImplementation(), gp);
                        break;
                    }
                }
            }

        } catch (InvalidRegistrationException e) {
            e.printStackTrace();
            throw new GameException (e);
        }

        return ret;
    }

    public ServiceGameEvent startNewGameWithAI (GridRegistrant registrant, Color preferredColor,
        String gameTypeStr, String mode, String [] strategies)
    {

        ServiceGameEvent ret = startNewGameWithAI (registrant, preferredColor, gameTypeStr, strategies);
        GridGame game = ret.getGame();
        if (game instanceof ManantialesGame) {
            ManantialesGame m = (ManantialesGame) game;
            m.setMode(Mode.valueOf(mode));
            ret.setGame(game);
        }

        /* Publish modified game */
        SharedBoardRemote sharedBoard = getSharedBoard();
        sharedBoard.shareGame(game);        

        return ret;                                
    }


    public List<GridGame> getUnfinishedGames(GridRegistrant player){
        RegistrarRemote registrar = getRegistrar();
        Collection<Game> games = registrar.getUnfinishedGames(
            new Registrant (player));
        List<GridGame> ret = new ArrayList<GridGame>();
        for (Game game : games) {
            ret.add((GridGame) game.getImplementation());
        }
        return ret;
    }

    public List<GridGame> getPendingGames(GridRegistrant player){
        RegistrarRemote registrar = getRegistrar();
        Collection<Game> games = registrar.getPendingGames(new Registrant (
            player));
        List<GridGame> ret = new ArrayList<GridGame>();
        for (Game game : games) {
           ret.add((GridGame) game.getImplementation());
        }
        return ret;
    }

    public GridGame getGame(int gameId) {
        SharedBoardRemote sharedBoard = getSharedBoard();
        Game game = sharedBoard.getGame(gameId);
        return (GridGame) game.getImplementation();
    }

    public GameGrid getGameGrid(int gameId) {
        SharedBoardRemote sharedBoard = getSharedBoard();
        Game game = sharedBoard.getGame(gameId);
        GridGame gridGame = (GridGame) game.getImplementation();
        return gridGame.getGrid();
    }

    public GridGame getPlayers(int gameId) {
        SharedBoardRemote sharedBoard = getSharedBoard();
        Game gameModel = sharedBoard.getGame(gameId);
        return (GridGame) gameModel.getImplementation();
    }

    public Move doMove(GridGame game, GridMove move) {
        SharedBoardRemote sharedBoard = getSharedBoard();
        try {
            return sharedBoard.doMove(new Game (game), new Move(move));
        } catch (InvalidMoveException e) {
            e.printStackTrace();
            throw new GameException(e);
        }
    }

    public Set<GridMove> getMoves(int gameId) {
        SharedBoardRemote sharedBoard = getSharedBoard();
        Game game = sharedBoard.getGame(gameId);
        GridGame impl = (GridGame) game.getImplementation();
        return impl.getMoves();
    }

    public GridMove updateMove(GridMove move) {
        SharedBoardRemote sharedBoard = getSharedBoard();
        Move moveModel = sharedBoard.updateMove(new Move(move));
        return (GridMove) moveModel.getImplementation();
    }

    /* Manantiales Specific Methods */

    public PuzzleSuggestion makeSuggestion (GridGame game, PuzzleSuggestion suggestion) throws
            InvalidSuggestionException
    {
        SharedBoardRemote sharedBoard = getSharedBoard();
        Suggestion ret = sharedBoard.makeSuggestion (new Game(game), new Suggestion(suggestion));
        return (PuzzleSuggestion) ret.getImplementation();
    }

    public Set<PuzzleSuggestion> getSuggestions (int gameId, SuggestionStatus status) {
        LinkedHashSet<PuzzleSuggestion> ret = new LinkedHashSet<PuzzleSuggestion>();
        Game gameModel = getSharedBoard().getGame(gameId);
        ManantialesGame game = (ManantialesGame) gameModel.getImplementation();
        for (PuzzleSuggestion sug : game.getSuggestions()) {
            if (sug.getStatus().equals(status))
                ret.add(sug);
        }

        return ret;

    }



}
