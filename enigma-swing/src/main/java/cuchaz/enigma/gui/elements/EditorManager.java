package cuchaz.enigma.gui.elements;

import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.function.Supplier;

import javax.annotation.Nullable;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import com.google.common.collect.HashBiMap;

import ModernDocking.DockingRegion;
import ModernDocking.event.DockingEvent;
import ModernDocking.event.DockingListener;
import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.classhandle.ClassHandle;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.events.EditorActionListener;
import cuchaz.enigma.gui.panels.ClosableTabTitlePane;
import cuchaz.enigma.gui.panels.DockablePanel;
import cuchaz.enigma.gui.panels.EditorPanel;
import cuchaz.enigma.gui.util.GuiUtil;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;

public class EditorManager {
	private final HashBiMap<ClassEntry, EditorPanel> editors = HashBiMap.create();
	private final EditorTabPopupMenu editorTabPopupMenu;
	private final Gui gui;
	private EditorPanel activeEditor;

	public EditorManager(Gui gui) {
		this.gui = gui;
		this.editorTabPopupMenu = new EditorTabPopupMenu(this);
	}

	public EditorPanel openClass(ClassEntry entry) {
		EditorPanel editorPanel = this.editors.computeIfAbsent(entry, e -> {
			Supplier<ClassHandle> classHandleSupplier = () -> this.gui.getController().getClassHandleProvider().openClass(entry);
			ClassHandle ch = classHandleSupplier.get();

			if (ch == null) {
				return null;
			}

			EditorPanel ed = new EditorPanel(this.gui, ch, this::onTabFocused);
			gui.getDockManager().addDockingListener(new DockingListener() {
				@Override
				public void dockingChange(DockingEvent e) {
					if (e.getDockable() != ed) return;

					switch (e.getID()) {
					case DOCKED:
						ed.setClassHandle(classHandleSupplier.get());
						ed.setup();
						break;
					case UNDOCKED:
						closedEditor(ed);
						break;
					default:
						break;
					}
				}
			});

			ed.addListener(new EditorActionListener() {
				@Override
				public void onCursorReferenceChanged(EditorPanel editor, EntryReference<Entry<?>, Entry<?>> ref) {
					if (editor == getActiveEditor()) {
						gui.showCursorReference(ref);
					}
				}

				@Override
				public void onClassHandleChanged(EditorPanel editor, ClassEntry old, ClassHandle ch) {
					EditorManager.this.editors.remove(old);
					EditorManager.this.editors.put(ch.getRef(), editor);
				}

				@Override
				public void onTitleChanged(EditorPanel editor, String title) {
					gui.getDockManager().updateTabInfo(editor.getUi());
				}
			});

			ed.getEditor().addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					if (e.getKeyCode() == KeyEvent.VK_4 && (e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0) {
						closeEditor(ed);
					}
				}
			});

			return ed;
		});

		if (editorPanel != null) {
			if (activeEditor == null) {
				gui.getDockManager().dock(editorPanel.getUi(), gui.getInfoPanel().getUi(), DockingRegion.SOUTH, 0.84);
			} else {
				gui.getDockManager().dock(editorPanel.getUi(), activeEditor.getUi(), DockingRegion.CENTER, 0.84);
			}

			gui.getDockManager().bringToFront(this.editors.get(entry).getUi());
			this.gui.showStructure(editorPanel);
		}

		return editorPanel;
	}

	public void closeEditor(EditorPanel ed) {
		gui.getDockManager().undock(ed.getUi());
		closedEditor(ed);
	}

	private void closedEditor(EditorPanel ed) {
		if (ed == activeEditor) {
			this.activeEditor = null;
		}

		this.editors.inverse().remove(ed);
		this.gui.showStructure(this.getActiveEditor());
		ed.destroy();
	}

	public void closeAllEditorTabs() {
		for (Iterator<EditorPanel> iter = this.editors.values().iterator(); iter.hasNext(); ) {
			EditorPanel e = iter.next();
			gui.getDockManager().undock(e.getUi());
			e.destroy();
			iter.remove();
		}

		this.activeEditor = null;
	}

	public void closeTabsLeftOf(EditorPanel ed) {
		// int index = this.openFiles.indexOfComponent(ed.getUi());

		// for (int i = index - 1; i >= 0; i--) {
		// 	closeEditor(EditorPanel.byUi(this.openFiles.getComponentAt(i)));
		// }
	}

	public void closeTabsRightOf(EditorPanel ed) {
		// int index = this.openFiles.indexOfComponent(ed.getUi());

		// for (int i = this.openFiles.getTabCount() - 1; i > index; i--) {
		// 	closeEditor(EditorPanel.byUi(this.openFiles.getComponentAt(i)));
		// }
	}

	public void closeTabsExcept(EditorPanel ed) {
		// int index = this.openFiles.indexOfComponent(ed.getUi());

		// for (int i = this.openFiles.getTabCount() - 1; i >= 0; i--) {
		// 	if (i == index) {
		// 		continue;
		// 	}

		// 	closeEditor(EditorPanel.byUi(this.openFiles.getComponentAt(i)));
		// }
	}

	@Nullable
	public EditorPanel getActiveEditor() {
		return activeEditor;
	}

	private void onTabPressed(EditorPanel editorPanel, MouseEvent e) {
		if (SwingUtilities.isRightMouseButton(e)) {
			this.editorTabPopupMenu.show(editorPanel.getUi(), e.getX(), e.getY(), editorPanel);
		}

		this.gui.showStructure(this.getActiveEditor());
	}

	private void onTabFocused(EditorPanel editorPanel) {
		this.activeEditor = editorPanel;
		this.gui.showStructure(editorPanel);
	}

	public void retranslateUi() {
		this.editorTabPopupMenu.retranslateUi();
		this.editors.values().forEach(EditorPanel::retranslateUi);
	}
}
