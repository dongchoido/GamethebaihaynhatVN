package hearthstone.ui;

import hearthstone.persistence.FileGameRepository;
import hearthstone.persistence.MatchRecord;
import hearthstone.persistence.UserStats;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class HistoryDialog extends JDialog {

    public HistoryDialog(JFrame owner, FileGameRepository repository) {
        super(owner, "Thống kê và lịch sử", true);
        setSize(760, 440);
        setLocationRelativeTo(owner);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Người chơi", createUsersPanel(repository.getUsers()));
        tabs.addTab("Lịch sử trận", createMatchesPanel(repository.getMatches()));
        add(tabs);
    }

    private JScrollPane createUsersPanel(List<UserStats> users) {
        DefaultTableModel model = new ReadOnlyTableModel(
                new String[]{"Người chơi", "Thắng", "Thua", "Tổng trận"}, 0);
        for (UserStats user : users) {
            model.addRow(new Object[]{
                    user.getUsername(),
                    user.getWins(),
                    user.getLosses(),
                    user.getWins() + user.getLosses()
            });
        }
        return tableScroll(model);
    }

    private JScrollPane createMatchesPanel(List<MatchRecord> matches) {
        DefaultTableModel model = new ReadOnlyTableModel(
                new String[]{"Thời gian", "Người chơi 1", "Người chơi 2", "Người thắng", "Lượt"}, 0);
        for (MatchRecord match : matches) {
            model.addRow(new Object[]{
                    match.getFormattedTime(),
                    match.player1(),
                    match.player2(),
                    match.winner(),
                    match.turns()
            });
        }
        return tableScroll(model);
    }

    private JScrollPane tableScroll(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(28);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setFont(table.getTableHeader().getFont().deriveFont(Font.BOLD));
        return new JScrollPane(table);
    }

    private static class ReadOnlyTableModel extends DefaultTableModel {
        ReadOnlyTableModel(Object[] columns, int rows) {
            super(columns, rows);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }
}
