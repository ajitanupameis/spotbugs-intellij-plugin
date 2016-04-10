/*
 * Copyright 2016 Andre Pfeiler
 *
 * This file is part of FindBugs-IDEA.
 *
 * FindBugs-IDEA is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FindBugs-IDEA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FindBugs-IDEA.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.twodividedbyzero.idea.findbugs.gui.settings;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.PropertyKey;
import org.twodividedbyzero.idea.findbugs.common.util.GuiUtil;
import org.twodividedbyzero.idea.findbugs.common.util.IdeaUtilImpl;
import org.twodividedbyzero.idea.findbugs.common.util.New;
import org.twodividedbyzero.idea.findbugs.resources.ResourcesLoader;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * TODO: collapse
 * PathMacroManager.getInstance
 * ModulePathMacroManager
 * <p>
 * TODO: edit
 * IDEA Sample:
 * "Path Variables"
 * TODO: validate
 */
final class FilterPane extends JPanel {
	private final String title;
	private JBTable table;
	private JScrollPane scrollPane;

	FilterPane(@NotNull @PropertyKey(resourceBundle = ResourcesLoader.BUNDLE) final String titleKey) {
		super(new BorderLayout());
		title = ResourcesLoader.getString(titleKey);
		setBorder(GuiUtil.createTitledBorder(title));
		table = GuiUtil.createCheckboxTable(
				new Model(New.<Item>arrayList()),
				Model.IS_ENABLED_COLUMN,
				new ActionListener() {
					@Override
					public void actionPerformed(final ActionEvent e) {
						swapEnabled();
					}
				}
		);

		final JPanel toolbar = ToolbarDecorator.createDecorator(table)
				.setAddAction(new AnActionButtonRunnable() {
					@Override
					public void run(final AnActionButton anActionButton) {
						final Project project = IdeaUtilImpl.getProject(anActionButton.getDataContext());
						doAdd(project);
					}
				})
				.setRemoveAction(new AnActionButtonRunnable() {
					@Override
					public void run(final AnActionButton anActionButton) {
						doRemove();
					}
				})
				.setAsUsualTopToolbar().createPanel();

		scrollPane = ScrollPaneFactory.createScrollPane(table);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		add(toolbar, BorderLayout.NORTH);
		add(scrollPane);
	}

	@NotNull
	private Model getModel() {
		return (Model) table.getModel();
	}

	private void swapEnabled() {
		final int rows[] = table.getSelectedRows();
		for (final int row : rows) {
			getModel().rows.get(row).enabled = !getModel().rows.get(row).enabled;
		}
		getModel().fireTableDataChanged();
	}

	private void doAdd(@Nullable final Project project) {
		final FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createMultipleFilesNoJarsDescriptor();
		descriptor.setTitle(title);
		descriptor.setDescription(ResourcesLoader.getString("filter.choose"));
		descriptor.withFileFilter(new Condition<VirtualFile>() {
			@Override
			public boolean value(final VirtualFile virtualFile) {
				return XmlFileType.DEFAULT_EXTENSION.equalsIgnoreCase(virtualFile.getExtension());
			}
		});

		final VirtualFile[] files = FileChooser.chooseFiles(descriptor, this, project, null);
		if (files.length > 0) {
			for (final VirtualFile virtualFile : files) {
				final File file = VfsUtilCore.virtualToIoFile(virtualFile);
				getModel().rows.add(new Item(file.getAbsolutePath(), true));
			}
			getModel().fireTableDataChanged();
		}
	}

	private void doRemove() {
		// TODO
	}

	@Override
	public void setEnabled(final boolean enabled) {
		super.setEnabled(enabled);
		table.setEnabled(enabled);
		scrollPane.setEnabled(enabled);
	}

	boolean isModified(@NotNull final Map<String, Boolean> map) {
		final List<Item> rows = getModel().rows;
		if (rows.size() != map.size()) {
			return true;
		}
		for (final Item row : rows) {
			final Boolean enabled = map.get(row.path);
			if (enabled == null || enabled != row.enabled) {
				return true;
			}
		}
		return false;
	}

	void apply(@NotNull final Map<String, Boolean> map) throws ConfigurationException {
		map.clear();
		StringBuilder error = null;
		for (final Item row : getModel().rows) {
			final File file = new File(row.path);
			if (!file.exists()) {
				if (error == null) {
					error = new StringBuilder();
				} else {
					error.append("\n");
				}
				error.append(ResourcesLoader.getString("error.path.exists", row.path));
			}
			if (!file.isFile()) {
				if (error == null) {
					error = new StringBuilder();
				} else {
					error.append("\n");
				}
				error.append(ResourcesLoader.getString("error.path.type", row.path));
			}
			if (!file.canRead()) {
				if (error == null) {
					error = new StringBuilder();
				} else {
					error.append("\n");
				}
				error.append(ResourcesLoader.getString("error.file.readable", row.path));
			}
			map.put(row.path, row.enabled);
		}
		if (error != null) {
			throw new ConfigurationException(error.toString());
		}
	}

	void reset(@NotNull final Map<String, Boolean> map) {
		final List<Item> rows = getModel().rows;
		rows.clear();
		for (final Map.Entry<String, Boolean> entry : map.entrySet()) {
			rows.add(new Item(entry.getKey(), entry.getValue()));
		}
		getModel().fireTableDataChanged();
	}

	private class Item {
		@NotNull
		private final String path;
		private boolean enabled;

		private Item(@NotNull final String path, final boolean enabled) {
			this.path = path;
			this.enabled = enabled;
		}
	}

	private class Model extends AbstractTableModel {
		private static final int IS_ENABLED_COLUMN = 0;
		private static final int NAME_COLUMN = 1;
		@NotNull
		private final List<Item> rows;

		private Model(@NotNull final List<Item> rows) {
			this.rows = rows;
		}

		@Override
		public int getRowCount() {
			return rows.size();
		}

		@Override
		public int getColumnCount() {
			return 2;
		}

		@Override
		public boolean isCellEditable(final int rowIndex, final int columnIndex) {
			return columnIndex == IS_ENABLED_COLUMN;
		}

		@Override
		public Class<?> getColumnClass(final int columnIndex) {
			switch (columnIndex) {
				case IS_ENABLED_COLUMN:
					return Boolean.class;
				case NAME_COLUMN:
					return String.class;
				default:
					throw new IllegalArgumentException("Column " + columnIndex);
			}
		}

		@Override
		public Object getValueAt(final int rowIndex, final int columnIndex) {
			switch (columnIndex) {
				case IS_ENABLED_COLUMN:
					return rows.get(rowIndex).enabled;
				case NAME_COLUMN:
					return rows.get(rowIndex).path;
				default:
					throw new IllegalArgumentException("Column " + columnIndex);
			}
		}

		@Override
		public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
			if (columnIndex == IS_ENABLED_COLUMN) {
				rows.get(rowIndex).enabled = (Boolean) aValue;
			}
		}
	}
}