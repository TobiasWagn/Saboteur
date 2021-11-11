package fop.model.board;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import fop.model.cards.CardAnchor;
import fop.model.cards.GoalCard;
import fop.model.cards.PathCard;
import fop.model.graph.Graph;

/**
 * 
 * Stellt das Wegelabyrinth als Liste von Karten und als Graph dar.
 *
 */
public class Gameboard {
	
	protected final Map<Position, PathCard> board = new HashMap<>();
	protected final Graph<BoardAnchor> graph = new Graph<>();
	
	/**
	 * Erstellt ein leeres Wegelabyrinth und platziert Start- sowie Zielkarten.
	 */
	public Gameboard() {
		clear();
	}
	
	/**
	 * Zum Debuggen kann hiermit der Graph ausgegeben werden.<br>
	 * Auf {@code http://webgraphviz.com/} kann der Code dargestellt werden.
	 */
	public void printGraph() {
		graph.toDotCode().forEach(System.out::println);
	}
	
	/**
	 * Leert das Wegelabyrinth.
	 */
	public void clear() {
		board.clear();
		graph.clear();
	}
	
	// add, remove //
	
	/**
	 * Setzt eine neue Wegekarte in das Wegelabyrinth.<br>
	 * Verbindet dabei alle Kanten des Graphen zu benachbarten Karten,
	 * sofern diese einen Knoten an der benachbarten Stelle besitzen.
	 * @param x x-Position im Wegelabyrinth
	 * @param y y-Position im Wegelabyrinth
	 * @param card die zu platzierende Wegekarte
	 */
	public void placeCard(int x, int y, PathCard card) {
		//checke ob karte möglich ist zu platzieren
		if(!card.isPathCard())
			return;
		//füge karte dem board zu
		board.put(Position.of(x, y), card);
		//für alle anchor der karte
		card.getGraph().vertices().forEach(anchor -> {
			//füge anchor dem graphen als knoten hinzu
			graph.addVertex(BoardAnchor.of(x,y,anchor));
			//füge kante zu knoten der nachbarn hinzu
			graph.addEdge(
				//knotenpunkt auf der karte	
				BoardAnchor.of(x, y, anchor),
				//knotenpunkt des nachbarn
				BoardAnchor.of(anchor.getAdjacentPosition(Position.of(x, y)), anchor.getOppositeAnchor()));});
		//füge kanten auf der karte dem graphen hinzu
		card.getGraph().edges().forEach(edge -> graph.addEdge(BoardAnchor.of(x, y, edge.x()), BoardAnchor.of(x, y, edge.y())));
		// stehen lassen
		// check for goal cards
		checkGoalCards();
	}
	
	/**
	 * Prüft, ob eine Zielkarte erreichbar ist und dreht diese gegebenenfalls um.
	 */
	private void checkGoalCards() {
		for (Entry<Position, PathCard> goal : board.entrySet().stream().filter(e -> e.getValue().isGoalCard()).collect(Collectors.toList())) {
			int x = goal.getKey().x();
			int y = goal.getKey().y();
			if (existsPathFromStartCard(x, y)) {
				GoalCard goalCard = (GoalCard) goal.getValue();
				if (goalCard.isCovered()) {
					// turn card
					goalCard.showFront();
					// generate graph to match all neighbor cards
					goalCard.generateGraph(card -> doesCardMatchItsNeighbors(x, y, card));
					// connect graph of card
					placeCard(x, y, goalCard);
				}
			}
		}
		
	}
	
	/**
	 * Entfernt die Wegekarte an der übergebenen Position.
	 * @param x x-Position im Wegelabyrinth
	 * @param y y-Position im Wegelabyrinth
	 * @return die Karte, die an der Position lag
	 */
	public PathCard removeCard(int x, int y) {
		//checke ob karte existiert
		if(this.isPositionEmpty(x, y))
			return null;
		//zu entfernende karte
		PathCard removeCard = board.get(Position.of(x, y));
		//entferne karte aus board
		board.remove(Position.of(x, y));
		//entferne alle knoten von karte und damit edges von karte und an karte vom graph
		removeCard.getGraph().vertices().forEach(anchor -> graph.removeVertex(BoardAnchor.of(x, y, anchor)));
		//rückgabe: enfernte karte
		return removeCard;
	}
	
	
	// can //
	
	/**
	 * Gibt genau dann {@code true} zurück, wenn die übergebene Karte an der übergebene Position platziert werden kann.
	 * @param x x-Position im Wegelabyrinth
	 * @param y y-Position im Wegelabyrinth
	 * @param card die zu testende Karte
	 * @return {@code true}, wenn die Karte dort platziert werden kann; sonst {@code false}
	 */
	public boolean canCardBePlacedAt(int x, int y, PathCard card) {
		return isPositionEmpty(x, y) && existsPathFromStartCard(x, y) && doesCardMatchItsNeighbors(x, y, card);
	}
	
	/**
	 * Gibt genau dann {@code true} zurück, wenn auf der übergebenen Position keine Karte liegt.
	 * @param x x-Position im Wegelabyrinth
	 * @param y y-Position im Wegelabyrinth
	 * @return {@code true}, wenn der Platz frei ist; sonst {@code false}
	 */
	private boolean isPositionEmpty(int x, int y) {
		return !board.containsKey(Position.of(x, y));
	}
	
	/**
	 * Gibt genau dann {@code true} zurück, wenn die übergebene Position von einer Startkarte aus erreicht werden kann.
	 * @param x x-Position im Wegelabyrinth
	 * @param y y-Position im Wegelabyrinth
	 * @return {@code true}, wenn die Position erreichbar ist; sonst {@code false}
	 */
	private boolean existsPathFromStartCard(int x, int y) {
		//position der startkarten
		List<Position> posOfStartcards = this.findStartCard();
		boolean pathExists = false;
		//gehe über alle positionen der startkarten
		for(Position posOfStartcard : posOfStartcards) {
			//gehe über alle seiten (top,left,right,bottom)
			for(CardAnchor anchor : CardAnchor.values()) {
				//nehme alle knoten der startkarte
				Iterator<CardAnchor> anchorIter = board.get(posOfStartcard).getGraph().vertices().iterator();
				//laufe über diese
				while(anchorIter.hasNext()) {
					CardAnchor startAnchor = anchorIter.next();
					//checke ob auf seite von position eine karte liegt
					if(board.containsKey(anchor.getAdjacentPosition(Position.of(x, y)))){
						//wenn ja, checke ob path von dort zur startkarte existiert
						if(graph.hasPath(
								BoardAnchor.of(anchor.getAdjacentPosition(Position.of(x, y)),anchor.getOppositeAnchor()),
								BoardAnchor.of(posOfStartcard, startAnchor))) {
							//dann existiert path
							pathExists = true;
							return pathExists;
						}
					}
				}	
			}
		}
		return pathExists;
	}
	
	/**
	 * Gibt genau dann {@code true} zurück, wenn die übergebene Karte an der übergebene Position zu ihren Nachbarn passt.
	 * @param x x-Position im Wegelabyrinth
	 * @param y y-Position im Wegelabyrinth
	 * @param card die zu testende Karte
	 * @return {@code true}, wenn die Karte dort zu ihren Nachbarn passt; sonst {@code false}
	 */
	private boolean doesCardMatchItsNeighbors(int x, int y, PathCard card) {
		//standardzustand
		boolean cardMatches = true;
		Set<CardAnchor> cardPathsAnchor = card.getGraph().vertices();
		//laufen über alle seiten
		for(CardAnchor anchor : CardAnchor.values()) {
			//angrenzende position
			Position adjacPosi = anchor.getAdjacentPosition(Position.of(x, y));
			//check ob karte auf angrenzender position existiert
			if(board.containsKey(adjacPosi)) {
				//angrenzende karte, wenn existent
				PathCard adjacCard = board.get(adjacPosi);
				//check ob goalcard
				if(adjacCard.isGoalCard()) {
					GoalCard adjacGoalCard = (GoalCard)adjacCard;
					//wenn goalcard, check ob covered, da dann behandelbar wie keine karte
					if(adjacGoalCard.isCovered())
						continue;
				}
				//check ob zu legende karte knotenpunkt an seite hat
				if(cardPathsAnchor.contains(anchor)) {
					//wenn ja, check ob angrenzende karte da auch knoten hat
					if(!adjacCard.getGraph().vertices().contains(anchor.getOppositeAnchor())) {
						cardMatches = false;
					}
					//wenn nein, check ob angrenzende karte da auch keinen knoten hat
				} else {
					if(adjacCard.getGraph().vertices().contains(anchor.getOppositeAnchor())) {
						cardMatches = false;
					}
				}	
			}			
		}	
		return cardMatches;
	}
	
	/**
	 * Gibt genau dann {@code true} zurück, wenn eine aufgedeckte Goldkarte im Wegelabyrinth liegt.
	 * @return {@code true} wenn eine Goldkarte aufgedeckt ist; sonst {@code false}
	 */
	public boolean isGoldCardVisible() {
		return board.values().stream().anyMatch(c -> c.isGoalCard() && ((GoalCard) c).getType() == GoalCard.Type.Gold && !((GoalCard) c).isCovered());
	}
	
	
	// get //
	
	public Map<Position, PathCard> getBoard() {
		return board;
	}
	
	public int getNumberOfAdjacentCards(int x, int y) {
		Set<Position> neighborPositions = Set.of(Position.of(x - 1, y), Position.of(x + 1, y), Position.of(x, y - 1), Position.of(x, y + 1));
		return (int) board.keySet().stream().filter(pos -> neighborPositions.contains(pos)).count();
	}
	
	public int getNumberOfCardsInGameBoardWOStartAndGoal() {
		int counter = 0;
		for(Position posi : board.keySet())
			if(!(board.get(posi).isGoalCard() || board.get(posi).isStartCard()))
				counter++;
		return counter;
	}
	
	/**
	 * Methode gibt die minimale Distanz aufgerunder als Integer der Karte an (x,y) 
	 * zu den Startkarten zurück (Distanz über Vektorberechnung)
	 * @param x x-Position der Karte
	 * @param y y-Position der Karte
	 * @return minimale Distanz zur Startkarte
	 */
	public int distanceFromStartcard(int x, int y) {
		return this.findStartCard().stream().mapToInt(posStart -> 
			(int)Math.ceil(
					Math.sqrt(
							(posStart.x()-x)*(posStart.x()-x) + (posStart.y()-y)*(posStart.y()-y)))).min().getAsInt();
	}

	public boolean atLeastOneGoalCardsIsVisible() {
		return board.values().stream().anyMatch(
				c -> c.isGoalCard() && !((GoalCard) c).isCovered());
	}
	
	/**
	 * Methode gibt Positionen der Karten des Boards zurück, die vom Typ StartCard ist.
	 * @return Position der Karte
	 */
	public List<Position> findStartCard() {
		//gehe über alle Positionen mit Karte
		List<Position> startCards = new ArrayList<>();
		Iterator<Position> PosiIter = board.keySet().iterator();
        while(PosiIter.hasNext()) {
        	//Position mit Karte
            Position posi = PosiIter.next();
            //check ob Startkarte
            if(board.get(posi).isStartCard()) {
            	startCards.add(posi);
            }
        }
        return startCards;
    }
	
	/**
	 * Methode gibt Positionen der Karten des Boards zurück, die vom Typ Goalcard ist.
	 * @return Positionen der Karte
	 */
	public List<Position> findGoalCard() {
		//gehe über alle Positionen mit Karte
		List<Position> goalCards = new ArrayList<>();
		Iterator<Position> PosiIter = board.keySet().iterator();
        while(PosiIter.hasNext()) {
        	//Position mit Karte
            Position posi = PosiIter.next();
            //check ob Startkarte
            if(board.get(posi).isGoalCard()) {
            	goalCards.add(posi);
            }
        }
        return goalCards;
    }

	public Position findGoldCard() {
		Position goldPos = null;
		Iterator<Position> PosiIter = board.keySet().iterator();
        while(PosiIter.hasNext()) {
        	//Position mit Karte
            Position posi = PosiIter.next();
  
            if(board.get(posi).isGoalCard()) {
            	GoalCard goalcard = (GoalCard)board.get(posi);
            	if(goalcard.getType() == GoalCard.Type.Gold)
            		goldPos = posi;
            }
        }
        return goldPos;
	}
	
}
