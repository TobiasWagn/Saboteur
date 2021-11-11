package fop.view.menu;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.AbstractTableModel;

import fop.io.ScoreEntryIO;
import fop.model.ScoreEntry;
import fop.view.MainFrame;

@SuppressWarnings("serial")
public class HighscoreView extends MenuView {
	
	
	public HighscoreView(MainFrame window) {
		super(window, "Highscores");
	}

	@Override
	protected void addContent(JPanel contentPanel) {
		contentPanel.setLayout(new GridBagLayout());
		
		// about text //
		GridBagConstraints scoreTableConstraints = new GridBagConstraints();
		scoreTableConstraints.weightx = 1.0;
		scoreTableConstraints.weighty = 1.0;
		scoreTableConstraints.fill = GridBagConstraints.BOTH;
		scoreTableConstraints.insets = new Insets(0, 5, 10, 5);
		scoreTableConstraints.gridx = 0;
		scoreTableConstraints.gridy = 0;
		
		scoreTableModel tableModel = new scoreTableModel(ScoreEntryIO.loadScoreEntries());
		JTable table = new JTable(tableModel);
		table.setPreferredScrollableViewportSize(new Dimension(500, 70));
//        table.setFillsViewportHeight(true);
        table.setBackground(contentPanel.getBackground().brighter());
        table.setSelectionBackground(new Color(200, 200, 200, 200));
        JScrollPane scrollPane = new JScrollPane(table,
        		ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
//        add(scrollPane);
        
        
        contentPanel.add(new JScrollPane(scrollPane), scoreTableConstraints);
		
		
		//back button
		GridBagConstraints rightImageConstraints = new GridBagConstraints();
		rightImageConstraints.insets = new Insets(2, 2, 0, 2);
		rightImageConstraints.gridx = 0;
		rightImageConstraints.gridy = 1;
		JButton backButton = createButton("Zurück");
		backButton.addActionListener(evt -> getWindow().setView(new MainMenu(getWindow())));
		contentPanel.add(backButton, rightImageConstraints);
		
	}
	
	private class scoreTableModel extends AbstractTableModel{
		
		private Object[][] data;
		
		private final List<ScoreEntry> scoreEntries;
		
	    public scoreTableModel(List<ScoreEntry> scoreEntries) {
	        this.scoreEntries = scoreEntries;
	        reload();
	    }
	    
	    public void reload() {
	    	DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
	        data = scoreEntries.stream().map(scoreentry ->{
	        	return new Object[] {
	        			scoreentry.getDateTime().format(formatter), 
	        			scoreentry.getName(), 
	        			Integer.toString(scoreentry.getScore())};
	        }).toArray(Object[][]::new);
	        fireTableDataChanged();
	    }

		@Override
		public int getRowCount() {
			return data.length;
		}

		@Override
		public int getColumnCount() {
			return 3;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			return data[rowIndex][columnIndex];
		}
		
		@Override
		    public boolean isCellEditable(int rowIndex, int columnIndex) {
		        return false;
		}
		
		@Override
        public Class<?> getColumnClass(int column) {
			switch (column) {
            	case 0: return String.class;
            	case 1: return String.class;
            	case 2: return String.class;
            	default: throw new IndexOutOfBoundsException(column);
			}	
        }
		
		@Override
	    public String getColumnName(int column) {
	        switch (column) {
	            case 0: return "Datum und Uhrzeit";
	            case 1: return "Name";
	            case 2: return "Punkte";
	            default: throw new IndexOutOfBoundsException(column);
	        }
	    }
	}
}
