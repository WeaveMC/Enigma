package cuchaz.enigma.gui.panels;

import java.awt.BorderLayout;
import java.util.Comparator;

import javax.swing.JScrollPane;

import cuchaz.enigma.gui.ClassSelector;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.utils.I18n;

public class ObfPanel extends DockablePanel {
	public final ClassSelector obfClasses;

	private final Gui gui;

	public ObfPanel(Gui gui) {
		super(getTranslatedTitle(), "obfuscated-classes-panel", gui.getDockManager());
		this.gui = gui;

		Comparator<ClassEntry> obfClassComparator = (a, b) -> {
			String aname = a.getFullName();
			String bname = b.getFullName();

			if (aname.length() != bname.length()) {
				return aname.length() - bname.length();
			}

			return aname.compareTo(bname);
		};

		this.obfClasses = new ClassSelector(gui, obfClassComparator, false);
		this.obfClasses.setSelectionListener(gui.getController()::navigateTo);
		this.obfClasses.setRenameSelectionListener(gui::onRenameFromClassTree);

		this.setLayout(new BorderLayout());
		this.add(new JScrollPane(this.obfClasses), BorderLayout.CENTER);

		this.retranslateUi();
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
