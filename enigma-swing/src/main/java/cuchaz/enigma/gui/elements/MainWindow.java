package cuchaz.enigma.gui.elements;

import java.awt.BorderLayout;
import java.awt.Container;

import javax.swing.JFrame;
import javax.swing.JMenuBar;

import ModernDocking.api.DockingAPI;
import ModernDocking.app.Docking;
import ModernDocking.app.RootDockingPanel;

public class MainWindow {
	private final JFrame frame;
	private final DockingAPI dockManager;

	private final JMenuBar menuBar = new JMenuBar();
	private final StatusBar statusBar = new StatusBar();

	public MainWindow(String title) {
		this.frame = new JFrame(title);
		this.frame.setJMenuBar(this.menuBar);
		this.dockManager = new Docking(frame);

		Container contentPane = this.frame.getContentPane();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(new RootDockingPanel(dockManager, frame), BorderLayout.CENTER);
		contentPane.add(this.statusBar.getUi(), BorderLayout.SOUTH);
	}

	public void setVisible(boolean visible) {
		this.frame.setVisible(visible);
	}

	public JMenuBar menuBar() {
		return this.menuBar;
	}

	public StatusBar statusBar() {
		return this.statusBar;
	}

	public DockingAPI dockManager() {
		return this.dockManager;
	}

	public JFrame frame() {
		return this.frame;
	}

	public void setTitle(String title) {
		this.frame.setTitle(title);
	}
}
