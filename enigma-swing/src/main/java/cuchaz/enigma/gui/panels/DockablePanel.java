package cuchaz.enigma.gui.panels;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.SwingConstants;

import ModernDocking.Dockable;
import ModernDocking.api.DockingAPI;
import ModernDocking.ui.DefaultHeaderUI;
import ModernDocking.ui.DockingHeaderUI;
import ModernDocking.ui.HeaderController;
import ModernDocking.ui.HeaderModel;

public class DockablePanel extends JPanel implements Dockable {
	private final String title;
	private final String persistentID;
	private HeaderController headerController;

	public DockablePanel(String title, String persistentID, DockingAPI dockManager) {
		super(new BorderLayout());

		this.title = title;
		this.persistentID = persistentID;
		dockManager.registerDockable(this);
	}

	@Override
	public String getPersistentID() {
		return persistentID;
	}

	@Override
	public String getTabText() {
		return title;
	}

	@Override
	public boolean isClosable() {
		return false;
	}

	@Override
	public boolean isWrappableInScrollpane() {
		return false;
	}

	@Override
	public DockingHeaderUI createHeaderUI(HeaderController headerController, HeaderModel headerModel) {
		this.headerController = headerController;
		return new DefaultHeaderUI(headerController, headerModel);
	}

	public static class WithTopTabs extends DockablePanel {
		public WithTopTabs(String title, String persistentID, DockingAPI dockManager) {
			super(title, persistentID, dockManager);
		}

		@Override
		public int getTabPosition() {
			return SwingConstants.TOP;
		}
	}
}

