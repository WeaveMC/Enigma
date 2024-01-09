package cuchaz.enigma.gui.panels;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import cuchaz.enigma.gui.ClassSelector;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.elements.DeobfPanelPopupMenu;
import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.utils.I18n;

public class DeobfPanel extends DockablePanel {
	public final ClassSelector deobfClasses;

	public final DeobfPanelPopupMenu deobfPanelPopupMenu;

	private final Gui gui;

	public DeobfPanel(Gui gui) {
		super(getTranslatedTitle(), "deobfuscated-classes-panel", gui.getDockManager());
		this.gui = gui;

		this.deobfClasses = new ClassSelector(gui, ClassSelector.DEOBF_CLASS_COMPARATOR, true);
		this.deobfClasses.setSelectionListener(gui.getController()::navigateTo);
		this.deobfClasses.setRenameSelectionListener(gui::onRenameFromClassTree);
		this.deobfPanelPopupMenu = new DeobfPanelPopupMenu(this);

		this.setLayout(new BorderLayout());
		this.add(new JScrollPane(this.deobfClasses), BorderLayout.CENTER);

		this.deobfClasses.addMouseListener(GuiUtil.onMousePress(this::onPress));

		this.retranslateUi();
	}

	private void onPress(MouseEvent e) {
		if (SwingUtilities.isRightMouseButton(e)) {
			deobfClasses.setSelectionRow(deobfClasses.getClosestRowForLocation(e.getX(), e.getY()));
			int i = deobfClasses.getRowForPath(deobfClasses.getSelectionPath());

			if (i != -1) {
				deobfPanelPopupMenu.show(deobfClasses, e.getX(), e.getY());
			}
		}
	}

	@Override
	public String getTabText() {
		return getTranslatedTitle();
	}

	private static String getTranslatedTitle() {
		return I18n.translate("info_panel.classes.obfuscated");
	}

	public void retranslateUi() {
		gui.getDockManager().updateTabInfo(this);
	}
}
