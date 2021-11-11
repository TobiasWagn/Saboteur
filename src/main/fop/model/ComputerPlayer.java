package fop.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import fop.controller.GameController;
import fop.model.board.Position;
import fop.model.cards.BrokenToolCard;
import fop.model.cards.Card;
import fop.model.cards.FixedToolCard;
import fop.model.cards.GoalCard;
import fop.model.cards.PathCard;

import javax.swing.SwingWorker;

/***
 * 
 * Stellt einen Computerspieler dar.
 *
 */
public class ComputerPlayer extends Player {
	
	private Boolean goalCardsInit = false;
	private List<Position> goalCards = new ArrayList<>();
	private Boolean goldcardFound = false;
	
	private static final List<String> PATHCARDNAMESGOLDMINER = List.of(
			"curve_double_down", "curve_down", "curve_up", "curve_up_dead_bottom", "line_horiz", "line_vert",
			"line_vert_dead_two", "plus", "three_horiz", "three_vert", "tunnel");;
	private static final List<String> PATHCARDNAMESSABOTEUR = List.of(
			"dead_curve_down", "dead_curve_up", "dead_line_horiz", "dead_line_vert","dead_plus", "dead_single_horiz",
			"dead_single_vert", "dead_three_horiz", "dead_three_vert");
	private static final List<String> PATHCARDNAMESMANAGER = List.of(
			"curve_double_down", "curve_down", "curve_up", "curve_up_dead_bottom", "dead_curve_down", "dead_curve_up",
			"dead_line_horiz", "dead_line_vert", "dead_plus", "dead_single_horiz", "dead_single_vert", "dead_three_horiz",
			"dead_three_vert", "line_horiz", "line_vert", "line_vert_dead_two", "plus", "three_horiz", "three_vert",
			"tunnel");

	private static final double GOLDMINERBREAKTOOLPROB = 0.2;
	private static final double SABOTEURBREAKTOOLPROB = 1.0;
	private static final double MANAGERBREAKTOOLPROB = 0.1;
	
	private static final double GOLDMINERFIXTOOLPROB = 0.8;
	private static final double SABOTEURFIXTOOLPROB = 0;
	private static final double MANAGERFIXTOOLPROB = 0.8;
	
	private boolean zuggemacht = false;
	
	public ComputerPlayer(String name) {
		super(name);
		
		GameController.addPropertyChangeListener(GameController.NEXT_PLAYER, evt -> {
			// skip if it is not the players turn
			if (GameController.getActivePlayer() != this) return;
			
			// do action in background worker
			new SwingWorker<Object, Void>() {
				
				@Override
				protected Object doInBackground() throws Exception {
					sleep(800);
					doAction();
					sleep(800);
					return null;
				}
			}.execute();
		});
	}
	
	@Override
	public boolean isComputer() {
		return true;
	}
	
	/**
	 * Pausiert das Programm, damit die Änderungen auf der Benutzeroberfläche sichtbar werden.
	 * @param timeMillis zu wartende Zeit in Millisekunden
	 */
	protected void sleep(int timeMillis) {
		try {
			TimeUnit.MILLISECONDS.sleep(timeMillis);
		} catch (InterruptedException ignored) {}
	}
	
	protected void selectCard(Card card) {
		GameController.selectCard(card);
		sleep(800);
	}
	
	/**
	 * Führt einen Zug des Computerspielers aus.<br>
	 * Benutzt {@link #selectCard(Card)}, um eine Karte auszuwählen.<br>
	 * Benutzt Methoden in {@link GameController}, um Aktionen auszuführen.
	 */
	protected void doAction() {
		
		zuggemacht = false;
		
		//Repariere kaputtes Wergzeug bei sich selbst wenn möglich
		if(this.hasBrokenTool()) {
			this.fixOwnTool();
		}
		if(zuggemacht)
			return;
		//Nutze Steinschlagkarte
		for(Card handcard : handCards) {
			
			if(handcard.isRockfall()) {
				selectCard(handcard);
				this.rockfall();
			}
			if(zuggemacht)
				return;
			
		}
		//Nutze Mapkarte
		for(Card handcard : handCards) {
			if(handcard.isMap()) {
				selectCard(handcard);
				this.mapCards();
			}
			if(zuggemacht)
				return;
		}
		//Nutze BreakTool
		if(Math.random() < this.getBreakToolProb()) {
			for(Card handcard : handCards) {
				if(handcard.isBrokenTool()) {
					selectCard(handcard);
					this.breakToolCard((BrokenToolCard)handcard);
				}
				if(zuggemacht)
				return;	
			}
		}
		//Nutze FixTool
		if(Math.random() < this.getFixToolProb()) {
			for(Card handcard : handCards) {
				if(handcard.isFixedTool()) {
					selectCard(handcard);
					this.fixToolCard((FixedToolCard)handcard);
				}
				if(zuggemacht)
					return;
			}
		}
		
		//Wenn BrokenTool, schmeiße random Karte aus.
		if(this.hasBrokenTool()) {
			this.discardRandomCard();
			if(zuggemacht)
				return;
		}
		
		//Nutze PathCards
		this.placePathCard();
		if(zuggemacht)
			return;
		
		//Wenn nichts klappt, werde random Karte ab;
		this.discardRandomCard();
		if(zuggemacht)
			return;
	}
	

	

	///////////////
	// USE CARDS //
	///////////////
	
	private void discardRandomCard() {
		Card handcard = handCards.get((int)(Math.random()*handCards.size()));
		selectCard(handcard);
		zuggemacht = true;
		GameController.discardSelectedCard(GameController.getActivePlayer());
		return;
	}
	
	private void fixOwnTool() {	
		for(Card handcard : this.getAllHandCards()) {
			if(handcard.isFixedTool()) {
				for(BrokenToolCard brokenToolCard : this.getBrokenTools()) {
					if(this.canBrokenToolBeFixed(brokenToolCard, (FixedToolCard)handcard)) {
						selectCard(handcard);
						zuggemacht = true;
						GameController.fixBrokenToolCardWithSelectedCard(this, this, brokenToolCard);
						return;
					}
				}
			}
		}
	}
	
	private void rockfall() {
		switch(this.getRole()) {
			case GOLD_MINER:
				zuggemacht = true;
				GameController.discardSelectedCard(GameController.getActivePlayer());
				return;
			case SABOTEUR:
			case MANAGER:
				Map<Position, Double> distance = new HashMap<>();
				Set<Position> allCards = GameController.getCardPositions();
				for(Position posi : allCards) {
					if(!GameController.getCardAt(posi).isGoalCard() && !GameController.getCardAt(posi).isStartCard()) {
						distance.put(posi, this.distFromNearestGoal(posi));
					}
				}
				if(distance.isEmpty()) {
					return;
				}
				Double minDist = Collections.min(distance.values());
				if(this.getRole() == Player.Role.MANAGER && minDist > 2)
					return;
				for(Position posi : distance.keySet()) {
					if(distance.get(posi) == minDist) {
						zuggemacht = true;
						GameController.destroyCardWithSelectedCardAt(GameController.getActivePlayer(), posi.x(), posi.y());	
						return;
					}
				}
		}
	}
	

	
	private void mapCards() {
		switch(this.getRole()) {
		case GOLD_MINER:
			if(goldcardFound == false) {
				if(goalCardsInit == false) {
					for(Position posi : GameController.getAllGoalCards())
						goalCards.add(posi);
					goalCardsInit = true;
				}	
				GoalCard goalcard = (GoalCard)GameController.getCardAt(goalCards.get(0));
				if(!goalcard.isCovered()) {
					goalCards.remove(0);
					return;
				}
				if(goalcard.getType() == GoalCard.Type.Gold) {
					goldcardFound = true;
				} else {
					goalCards.remove(0);
				}
				zuggemacht = true;
				GameController.lookAtGoalCardWithSelectedCard(GameController.getActivePlayer(), goalcard);
				return;
			} else {
				zuggemacht = true;
				GameController.discardSelectedCard(GameController.getActivePlayer());
				return;
			}
		case SABOTEUR:
		case MANAGER:
			zuggemacht = true;
			GameController.discardSelectedCard(GameController.getActivePlayer());
			return;
		}
		
	}
	
	
	
	private void breakToolCard(BrokenToolCard brokenToolCard) {
		List<Player> players = Arrays.asList(GameController.getPlayers());
		Collections.shuffle(players);
		for(Player player : players) {
			if(player == GameController.getActivePlayer())
				continue;
			if(player.canToolBeBroken(brokenToolCard)) {
				zuggemacht = true;
				GameController.breakToolWithSelectedCard(GameController.getActivePlayer(), player);
				return;
			}
		}
	}
	
	private void fixToolCard(FixedToolCard handcard) {
		List<Player> players = Arrays.asList(GameController.getPlayers());
		Collections.shuffle(players);
		for(Player player : players) {
			if(player == GameController.getActivePlayer())
				continue;
			if(!player.getBrokenTools().isEmpty()) {
				for(BrokenToolCard brokenTool : player.getBrokenTools()) {
					if(player.canBrokenToolBeFixed(brokenTool, handcard)) {
						zuggemacht = true;
						GameController.fixBrokenToolCardWithSelectedCard(GameController.getActivePlayer(), player, brokenTool);
						return;
					}
				}
			}
		}
	}
	
	private void placePathCard() {
		List<Card> niceCards = handCards.stream()
				.filter(
				hc -> this.getNicePathCards().contains(hc.getName().substring(0, hc.getName().length()-2)))
				.collect(Collectors.toList());
		if(!niceCards.isEmpty()) {
			Position bestPosition = null;
			Double shortestDistance = Double.POSITIVE_INFINITY;
			Card bestcard = null;
			for(Card handcard : niceCards) {
				double[] distAndPos;
				if(!goldcardFound)
					distAndPos = this.getNiceDistToGoalCards(handcard);
				else 
					distAndPos = this.getNiceDistToGoldCard(handcard);
				if(distAndPos == null) {
					continue;
				}
				if(distAndPos[0] < shortestDistance) {
					shortestDistance = distAndPos[0];
					bestPosition = Position.of((int)distAndPos[1], (int)distAndPos[2]);
					bestcard = handcard;
				}
			}
			if(bestcard == null || bestPosition == null) {
				return;
			}
			selectCard(bestcard);
			zuggemacht = true;
			GameController.placeSelectedCardAt(GameController.getActivePlayer(), bestPosition.x(), bestPosition.y());
			return;
		}
	}
	
	
	

	/////////////////////
	// NEBENFUNKTIONEN //
	/////////////////////
	
	private double distFromNearestGoal(Position position) {
		double distance = Double.POSITIVE_INFINITY;
		for(Position goalPosi : GameController.getAllGoalCards()) {
			double newDistance = Math.sqrt(
					(goalPosi.x()-position.x())*(goalPosi.x()-position.x()) +
					(goalPosi.y()-position.y())*(goalPosi.y()-position.y()));
			if(distance > newDistance)
				distance = newDistance;
		}
		return distance;
	}
	
	
	private double getBreakToolProb() {
		switch(this.getRole()) {
		case GOLD_MINER: return GOLDMINERBREAKTOOLPROB;
		case SABOTEUR: return SABOTEURBREAKTOOLPROB;
		case MANAGER: return MANAGERBREAKTOOLPROB;
		default: return 0;
		}
	}
	
	private double getFixToolProb() {
		switch(this.getRole()) {
		case GOLD_MINER: return GOLDMINERFIXTOOLPROB;
		case SABOTEUR: return SABOTEURFIXTOOLPROB;
		case MANAGER: return MANAGERFIXTOOLPROB;
		default: return 0;
		}
	}
	
	private List<String> getNicePathCards() {
		switch(this.getRole()) {
		case GOLD_MINER: return ComputerPlayer.PATHCARDNAMESGOLDMINER;
		case SABOTEUR: return ComputerPlayer.PATHCARDNAMESSABOTEUR;
		case MANAGER: return ComputerPlayer.PATHCARDNAMESMANAGER;
		default: return ComputerPlayer.PATHCARDNAMESMANAGER;
		}	
	}
	
	private double[] getNiceDistToGoalCards(Card card) {
		Map<Position, Double> distance = new HashMap<>();
		Set<Position> validPositions = this.getValidPositions((PathCard)card);
		for(Position pos : validPositions) {
			distance.put(pos, this.distFromNearestGoal(pos));
		}
		if(distance.isEmpty()) {
			return null;
		}
		double[] ergebnis = new double[3];
		double minDist = 0.0;
		switch(this.getRole()) {
		case GOLD_MINER:
		case SABOTEUR:
			minDist = Collections.min(distance.values()); break;
		case MANAGER:
			minDist = Collections.max(distance.values()); break;
		}

		ergebnis[0] = minDist;
		for(Position posi : distance.keySet()) {
			if(distance.get(posi) == minDist) {
				ergebnis[1] = posi.x();
				ergebnis[2] = posi.y();
				return ergebnis;
			}
		}
		return null;
	}
	
	private double[] getNiceDistToGoldCard(Card card) {
	
		Map<Position, Double> distance = new HashMap<>();
		for(Position pos : this.getValidPositions((PathCard)card)) {
			distance.put(pos, this.distFromGoldCard(pos));
		}
		if(distance.isEmpty()) {
			return null;
		}
		double[] ergebnis = new double[3];
		double minDist = 0.0;
		switch(this.getRole()) {
		case GOLD_MINER:
		case SABOTEUR:
			minDist = Collections.min(distance.values()); break;
		case MANAGER:
			minDist = Collections.max(distance.values()); break;
		}
		ergebnis[0] = minDist;
		for(Position posi : distance.keySet()) {
			if(distance.get(posi) == minDist) {
				ergebnis[1] = posi.x();
				ergebnis[2] = posi.y();
				return ergebnis;
			}
		}
		return null;
	}
	
	private Double distFromGoldCard(Position position) {
		double distance = Double.POSITIVE_INFINITY;
		Position goldPosi = GameController.getPosGoldCard();
		if(goldPosi == null)
			return 0.0;
		double newDistance = Math.sqrt(
				(goldPosi.x()-position.x())*(goldPosi.x()-position.x()) +
				(goldPosi.y()-position.y())*(goldPosi.y()-position.y()));
		if(distance > newDistance)
			distance = newDistance;
		return distance;
	}

	private Set<Position> getValidPositions(PathCard pathcard) {
		Set<Position> validPositions = new HashSet<>();
		Set<Position> cardPositions = GameController.getCardPositions();
		int minX = cardPositions.stream().mapToInt(Position::x).min().orElse(0) - 1;
		int maxX = cardPositions.stream().mapToInt(Position::x).max().orElse(0) + 1;
		int minY = cardPositions.stream().mapToInt(Position::y).min().orElse(0) - 1;
		int maxY = cardPositions.stream().mapToInt(Position::y).max().orElse(0) + 1;		
		
		// collect all valid positions
		for (int x = minX; x <= maxX; x++)
			for (int y = minY; y <= maxY; y++)
				if (GameController.canCardBePlacedAt(x, y, pathcard))
					validPositions.add(Position.of(x, y));
		return validPositions;
	}
	
}
