package fop.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import fop.model.ScoreEntry;

/**
 * 
 * Wird genutzt, um {@link ScoreEntry} Objekte zu schreiben und zu lesen.<br>
 * <br>
 * Es handelt sich um die Datei {@value #PATH}.<br>
 * Mit {@link #loadScoreEntries()} werden die Elemente gelesen.<br>
 * Mit {@link #writeScoreEntries(List)} werden die Elemente geschrieben.
 *
 */
public final class ScoreEntryIO {
	
	/** Der Pfad zur ScoreEntry Datei */
	private static String PATH = "highscores.txt";
	
	private ScoreEntryIO() {}
	
	/**
	 * Liest eine Liste von {@link ScoreEntry} Objekten aus der Datei {@value #PATH}.<br>
	 * Die Liste enthält die Elemente in der Reihenfolge, in der sie in der Datei vorkommen.<br>
	 * Ungültige Einträge werden nicht zurückgegeben.
	 * @return die ScoreEntry Objekte
	 */
	public static List<ScoreEntry> loadScoreEntries() {
		String line;
		List<ScoreEntry> scoreList = new ArrayList<ScoreEntry>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(PATH));
			while((line = reader.readLine()) != null) {
				ScoreEntry scoreLine = ScoreEntry.read(line);
				if(scoreLine != null)
					scoreList.add(scoreLine);
			}
			reader.close();
			return scoreList;
		}
		catch(IOException e) {
			return scoreList;
		}
	}
	
	/**
	 * Schreibt eine Liste von {@link ScoreEntry} Objekten in die Datei {@value #PATH}.<br>
	 * Die Elemente werden in der Reihenfolge in die Datei geschrieben, in der sie in der Liste vorkommen.
	 * @param scoreEntries die zu schreibenden ScoreEntry Objekte
	 */
	public static void writeScoreEntries(List<ScoreEntry> scoreEntries) {
		try {
			PrintWriter printWriter = new PrintWriter(new FileWriter(PATH));
			for(ScoreEntry score : scoreEntries) {
				score.write(printWriter);
			}
			printWriter.close();
		} 
		catch (IOException e) {
			return;
		} 
	}
	
	/**
	 * Schreibt das übergebene {@link ScoreEntry} Objekt an der korrekten Stelle in die Datei {@value #PATH}.<br>
	 * Die Elemente sollen absteigend sortiert sein. Wenn das übergebene Element dieselbe Punktzahl wie ein
	 * Element der Datei hat, soll das übergebene Element danach eingefügt werden.
	 * @param scoreEntry das ScoreEntry Objekt, das hinzugefügt werden soll
	 */
	public static void addScoreEntry(ScoreEntry scoreEntry) {
		int index = 0;
		List<ScoreEntry> scoreEntries = ScoreEntryIO.loadScoreEntries();
		for(ScoreEntry score : scoreEntries) {
			if(score.compareTo(scoreEntry)>=0)
				index++;
			else break;
		}
		scoreEntries.add(index, scoreEntry);
		ScoreEntryIO.writeScoreEntries(scoreEntries);
	}
	
}
