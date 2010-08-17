package mx.ecosur.multigame.impl.entity.pasale;

import mx.ecosur.multigame.enums.GameState;

import mx.ecosur.multigame.exception.InvalidMoveException;
import mx.ecosur.multigame.exception.InvalidRegistrationException;

import mx.ecosur.multigame.impl.Color;
import mx.ecosur.multigame.impl.MoveComparator;
import mx.ecosur.multigame.impl.enums.pasale.TokenType;
import mx.ecosur.multigame.impl.model.*;

import mx.ecosur.multigame.model.implementation.AgentImpl;
import mx.ecosur.multigame.model.implementation.GamePlayerImpl;
import mx.ecosur.multigame.model.implementation.Implementation;
import mx.ecosur.multigame.model.implementation.MoveImpl;
import mx.ecosur.multigame.model.implementation.RegistrantImpl;
import static mx.ecosur.multigame.impl.util.pasale.RuleFunctions.*;
import mx.ecosur.multigame.MessageSender;

import javax.persistence.*;

import org.drools.io.Resource;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.io.ResourceFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.KnowledgeBaseFactory;
import org.drools.KnowledgeBase;
import org.drools.audit.WorkingMemoryFileLogger;

import java.util.*;
import java.util.List;
import java.net.MalformedURLException;
import java.awt.*;

@Entity
public class PasaleGame extends GridGame {

    private static final long serialVersionUID = -8395074059039838349L;

    private static final String ChangeSet = "/mx/ecosur/multigame/impl/tablon.xml";

    private static final boolean DEBUG = false;

    private static final int DIMENSIONS = 20;  // so, DIMENSIONS x DIMENSIONS

    private static KnowledgeBase kbase;

    private transient MessageSender messageSender;

    private StatefulKnowledgeSession session;

    private WorkingMemoryFileLogger logger;

    public PasaleGame() {
        super();
        setRows (DIMENSIONS);
        setColumns(DIMENSIONS);
        setCreated(new Date());
        setState(GameState.PLAY);
        if (grid.getCells().size() == 0)
            grid = createGrid();
    }


    public PasaleGame(int columns, int rows) {
        this();
        setColumns(columns);
        setRows(rows);
        kbase = null;
    }

    public PasaleGame(int columns, int rows, KnowledgeBase kbase) {
        this(columns, rows);
        PasaleGame.kbase = kbase;
    }

    @Transient
    public Dimension getDimensions() {
        return new Dimension (getColumns(), getRows());
    }

    private PasaleGrid createGrid() {
        PasaleGrid grid = new PasaleGrid ();
        int lower = (DIMENSIONS / 2) - 2;
        int upper = (DIMENSIONS / 2)  + 2;
        /* Populate the grid */
        for (int col = 0; col <= DIMENSIONS; col++) {
            for (int row = 0; row <= DIMENSIONS; row++) {
                if ( (col + row) % 2 != 0)
                    continue;
                if ( row % 2 == 1 || col % 2 == 1) {
                /* soil or water */
                /* TODO:  Dynamically determine the location of rivers on the map */
                if ( (col > lower && col < upper) || (row > lower && row < upper)) {
                    PasaleFicha particle = null;
                    if ( (col == 1 || col == upper) || (row == 1 || row == upper) ) {
                       particle = new PasaleFicha (col, row, Color.UNKNOWN, TokenType.SOIL_PARTICLE);
                    } else {
                        particle = new PasaleFicha (col, row, Color.UNKNOWN, TokenType.WATER_PARTICLE);
                    }

                    grid.updateCell(particle);

                } else {
                    PasaleFicha soilParticle = new PasaleFicha (col, row, Color.UNKNOWN, TokenType.SOIL_PARTICLE);
                    grid.updateCell (soilParticle);
                }

                } else {
                    PasaleFicha forest = new PasaleFicha (col, row, Color.UNKNOWN, TokenType.FOREST);
                    grid.updateCell (forest);
                }
            }
        }

        return grid;        
    }



    @Transient
    public int getMaxPlayers() {
        return 4;
    }

    public void setMaxPlayers(int maxPlayers) {
        // do nothing
    }

    /* (non-Javadoc)
      * @see mx.ecosur.multigame.impl.model.GridGame#move(mx.ecosur.multigame.model.implementation.MoveImpl)
      */
    public MoveImpl move(MoveImpl move) throws InvalidMoveException {
        if (kbase == null) {
            kbase = findKBase();
        }
        
        if (session == null) {
            session = kbase.newStatefulKnowledgeSession();
            session.setGlobal("messageSender", getMessageSender());
            session.setGlobal("dimension", new Integer(getColumns()));
            session.insert(this);            
        }

        if (DEBUG) {
            logger = new WorkingMemoryFileLogger(session);
            logger.setFileName("audit");
        }

        session.insert(move);
        session.startProcess("tablon-flow");
        session.fireAllRules();
        if (logger != null)
            logger.writeToDisk();

        if (moves == null)
            moves = new TreeSet<GridMove>(new MoveComparator());

        moves.add((PasaleMove) move);

        return move;
    }

    public GamePlayerImpl registerPlayer(RegistrantImpl registrant) throws InvalidRegistrationException  {
        PasalePlayer player = new PasalePlayer();
        player.setRegistrant((GridRegistrant) registrant);

        for (GridPlayer p : this.getPlayers()) {
            if (p.equals (player))
                throw new InvalidRegistrationException ("Duplicate Registraton!");
        }

        int max = getMaxPlayers();
        if (players.size() == max)
            throw new RuntimeException ("Maximum Players reached!");

        List<Color> colors = getAvailableColors();
        player.setColor(colors.get(0));

        /* TODO: rethink "turns" in Pasale */
        if (player.getColor().equals(Color.YELLOW))
            player.setTurn(true);
        players.add(player);

        return player;
    }

    public AgentImpl registerAgent (AgentImpl agent) throws InvalidRegistrationException {
        throw new InvalidRegistrationException (
                "Agents cannot be registered with an Oculto Game!");
    }

    /* (non-Javadoc)
     * @see mx.ecosur.multigame.impl.model.GridGame#getColors()
     */
    @Override
    @Transient
    public List<Color> getColors() {
        List<Color> ret = new ArrayList<Color>();
        for (Color color : Color.values()) {
            if (color.equals(Color.UNKNOWN))
                continue;
            if (color.equals(Color.GREEN))
                continue;
            ret.add(color);
        }

        return ret;
    }


    @Transient
    public MessageSender getMessageSender() {
        if (messageSender == null) {
            messageSender = new MessageSender ();
            messageSender.initialize();
        }
        return messageSender;
    }

    public void setMessageSender(MessageSender messageSender) {
        this.messageSender = messageSender;
    }    


    @Transient
    public Resource getResource() {
        return ResourceFactory.newInputStreamResource(getClass().getResourceAsStream (
            "/mx/ecosur/multigame/impl/pasale.xml"));
    }

    @Transient
    public String getGameType() {
        return "Pasale";
    }

    @Deprecated
    protected KnowledgeBase findKBase () {
        KnowledgeBase ret = KnowledgeBaseFactory.newKnowledgeBase();
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        kbuilder.add(ResourceFactory.newInputStreamResource(getClass().getResourceAsStream (
            "/mx/ecosur/multigame/impl/pasale.xml")), ResourceType.CHANGE_SET);
        ret.addKnowledgePackages(kbuilder.getKnowledgePackages());
        return ret;
    }

    /* (non-Javadoc)
     * @see mx.ecosur.multigame.impl.model.GridGame#clone()
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        super.clone();
        PasaleGame ret = new PasaleGame();
        ret.setPlayers (this.getPlayers());
        ret.setGrid((GameGrid) grid.clone());
        ret.setColumns(this.getColumns());
        ret.setRows (this.getRows());
        ret.setGameType(this.getGameType());
        ret.setState(this.getState());
        ret.setMessageSender(this.getMessageSender());
        ret.setCreated(new Date (System.currentTimeMillis()));
        ret.setId(this.getId());
        ret.setMoves(this.getMoves());
        ret.setVersion(this.getVersion());
        return ret;
    }

    @Override
    public String toString() {
        PasaleGrid tgrid = (PasaleGrid) getGrid();
        StringBuffer ret = new StringBuffer("PasaleGame (id=" + id + ")\n");
        for (int y = 0; y < getColumns(); y++) {
            for (int x = 0; x < getRows(); x++) {
                GridCell cell = grid.getLocation (new GridCell (y,x, Color.UNKNOWN));
                if (cell != null) {
                    PasaleFicha ficha = (PasaleFicha) cell;
                    switch (ficha.getType()) {
                        case SOIL_PARTICLE:
                            ret.append("S");
                            break;
                        case FOREST:
                            if (isDirectlyConnectedToWater(ficha, tgrid.getSquare(ficha)))
                                ret.append("R");
                            else
                                ret.append("F");
                            break;
                        case POTRERO:
                            ret.append("P");
                            break;
                        case SILVOPASTORAL:
                            ret.append("V");
                            break;
                        case WATER_PARTICLE:
                            ret.append("W");
                            break;
                        default:
                            ret.append ("U");
                            break;
                    }
                } else {
                    ret.append(" ");
                }

                /* space out the cells */
                ret.append (" ");
            }
            ret.append("\n");
        }

        return ret.toString();
    }

    public void finalize() {
        try {
            super.finalize();
        } catch (Throwable t) {
           t.printStackTrace();
        }
        
        if (this.session != null)
            session.dispose();
    }
}
