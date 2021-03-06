/*
 * Copyright 2020 SpotBugs plugin contributors
 *
 * This file is part of IntelliJ SpotBugs plugin.
 *
 * IntelliJ SpotBugs plugin is free software: you can redistribute it 
 * and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 *
 * IntelliJ SpotBugs plugin is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied 
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with IntelliJ SpotBugs plugin.
 * If not, see <http://www.gnu.org/licenses/>.
 */
package org.jetbrains.plugins.spotbugs.gui.settings;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Processor;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.DetectorFactory;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.I18N;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.spotbugs.common.FindBugsPluginConstants;
import org.jetbrains.plugins.spotbugs.common.util.WithPluginClassloader;
import org.jetbrains.plugins.spotbugs.core.AbstractSettings;
import org.jetbrains.plugins.spotbugs.core.PluginSettings;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

abstract class AbstractDetectorNode extends DefaultMutableTreeNode {
	AbstractDetectorNode(@NotNull final String text) {
		super(text);
	}

	abstract boolean isGroup();

	@Nullable
	abstract Boolean getEnabled();

	abstract void setEnabled(@Nullable Boolean enabled);

	@NotNull
	private static AbstractDetectorNode createRoot(@NotNull final String text, @NotNull final Map<String, Map<String, Boolean>> enabled) {
		return new DetectorRootNode(text, enabled);
	}

	@NotNull
	private static DetectorNode create(@NotNull final DetectorFactory detector, @NotNull final Map<String, Map<String, Boolean>> enabled) {
		return new DetectorNode(detector, enabled);
	}

	@NotNull
	private static AbstractDetectorNode createGroup(@NotNull final String text) {
		return new DetectorGroupNode(text);
	}

	@NotNull
	static AbstractDetectorNode notLoaded() {
		return createRoot("not loaded", Collections.emptyMap());
	}

	@NotNull
	static AbstractDetectorNode buildRoot(
			@NotNull final DetectorGroupBy groupBy,
			@NotNull final Processor<DetectorFactory> acceptor,
			@NotNull final Map<String, Map<String, Boolean>> detectors
	) {

		final Map<String, List<DetectorNode>> byGroup = new HashMap<>();
		final Iterator<DetectorFactory> detectorFactoryIterator = WithPluginClassloader.notNull(
				() -> DetectorFactoryCollection.instance().factoryIterator());
		fillByGroup(groupBy, acceptor, detectorFactoryIterator, byGroup, detectors);

		final Comparator<DetectorNode> nodeComparator = (o1, o2) -> o1.toString().compareToIgnoreCase(o2.toString());

		final AbstractDetectorNode root = createRoot(groupBy.getDisplayName(), detectors);
		final ArrayList<String> groupSorted = new ArrayList<>(byGroup.keySet());
		Collections.sort(groupSorted);
		for (final String group : groupSorted) {
			final AbstractDetectorNode groupNode = createGroup(group);
			root.add(groupNode);
			final List<DetectorNode> nodes = new ArrayList<>(byGroup.get(group));
			nodes.sort(nodeComparator);
			for (final DetectorNode node : nodes) {
				groupNode.add(node);
			}
		}
		return root;
	}

	@NotNull
	private static Map<String, List<DetectorNode>> fillByGroup(
			@NotNull final DetectorGroupBy groupBy,
			@NotNull final Processor<DetectorFactory> acceptor,
			@NotNull final Iterator<DetectorFactory> iterator,
			@NotNull final Map<String, List<DetectorNode>> byGroup,
			@NotNull final Map<String, Map<String, Boolean>> enabledMap
	) {

		while (iterator.hasNext()) {
			final DetectorFactory factory = iterator.next();
			if (acceptor.process(factory)) {

				String group;
				switch (groupBy) {
					case Provider:
						group = factory.getPlugin().getProvider();
						if (group == null) {
							group = "Unknown";
						} else if (group.endsWith(" project")) {
							group = group.substring(0, group.length() - " project".length());
						}
						addNode(factory, group, byGroup, enabledMap);
						break;
					case Speed:
						group = factory.getSpeed();
						if (group == null) {
							group = "Unknown";
						}
						addNode(factory, group, byGroup, enabledMap);
						break;
					case BugCategory:
						final Collection<BugPattern> patterns = factory.getReportedBugPatterns();
						final Set<String> categories = new HashSet<>();
						for (final BugPattern bugPattern : patterns) {
							final String category = bugPattern.getCategory();
							if (!StringUtil.isEmptyOrSpaces(category)) {
								categories.add(category);
							}
						}
						if (categories.isEmpty()) {
							group = "Unknown";
							addNode(factory, group, byGroup, enabledMap);
						} else {
							for (final String category : categories) {
								group = I18N.instance().getBugCategoryDescription(category);
								if (StringUtil.isEmptyOrSpaces(group)) {
									group = category;
								}
								addNode(factory, group, byGroup, enabledMap);
							}
						}
						break;
					default:
						throw new UnsupportedOperationException("Unsupported " + groupBy);
				}

			}
		}
		return byGroup;
	}

	private static void addNode(
			@NotNull final DetectorFactory factory,
			@NotNull final String group,
			@NotNull final Map<String, List<DetectorNode>> byGroup,
			@NotNull final Map<String, Map<String, Boolean>> enabledMap
	) {
		List<DetectorNode> detectorNodes = byGroup.computeIfAbsent(group, k -> new ArrayList<>());
		detectorNodes.add(create(factory, enabledMap));
	}

	@NotNull
	static Map<String, Map<String, Boolean>> createEnabledMap(
			@NotNull final AbstractSettings settings
	) {
		final Map<String, Map<String, Boolean>> ret = new HashMap<>();
		if (!settings.detectors.isEmpty()) {
			ret.put(FindBugsPluginConstants.FINDBUGS_CORE_PLUGIN_ID, new HashMap<>(settings.detectors));
		}
		for (final PluginSettings pluginSettings : settings.plugins) {
			if (!pluginSettings.detectors.isEmpty()) {
				ret.put(pluginSettings.id, new HashMap<>(pluginSettings.detectors));
			}
		}
		return ret;
	}

	static void fillSettings(
			@NotNull final AbstractSettings settings,
			@NotNull final Map<String, Map<String, Boolean>> detectors
	) {
		apply(detectors.get(FindBugsPluginConstants.FINDBUGS_CORE_PLUGIN_ID), settings.detectors);
		for (final PluginSettings pluginSettings : settings.plugins) {
			apply(detectors.get(pluginSettings.id), pluginSettings.detectors);
		}
	}

	static void apply(
			@Nullable final Map<String, Boolean> from,
			@NotNull final Map<String, Boolean> to
	) {
		to.clear();
		if (from != null) {
			for (final Map.Entry<String, Boolean> entry : from.entrySet()) {
				to.put(entry.getKey(), entry.getValue());
			}
		}
	}
}
