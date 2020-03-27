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
package org.jetbrains.plugins.spotbugs.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ex.ApplicationInfoEx;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.BalloonBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.text.DateFormatUtil;
import edu.umd.cs.findbugs.Version;
import icons.PluginIcons;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.spotbugs.common.VersionManager;
import org.jetbrains.plugins.spotbugs.common.util.FindBugsUtil;
import org.jetbrains.plugins.spotbugs.core.FindBugsState;
import org.jetbrains.plugins.spotbugs.gui.common.BalloonTipFactory;
import org.jetbrains.plugins.spotbugs.resources.ResourcesLoader;

import javax.swing.event.HyperlinkEvent;
import java.awt.Component;
import java.awt.Point;
import java.awt.datatransfer.StringSelection;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;

public final class HelpAction extends AbstractAction {
	// TODO: fill proper values
	private static final String DOWNLOADS_WEBSITE = "www.google.com";

	private static final String A_HREF_COPY = "#copy";

	@Override
	void updateImpl(
			@NotNull final AnActionEvent e,
			@NotNull final Project project,
			@NotNull final ToolWindow toolWindow,
			@NotNull final FindBugsState state
	) {

		e.getPresentation().setEnabled(true);
		e.getPresentation().setVisible(true);
	}

	@Override
	void actionPerformedImpl(
			@NotNull final AnActionEvent e,
			@NotNull final Project project,
			@NotNull final ToolWindow toolWindow,
			@NotNull final FindBugsState state
	) {

		final Component parent = e.getInputEvent().getComponent();
		final BalloonBuilder builder = JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(
				createHelpInfo().toString(),
				PluginIcons.FINDBUGS_ICON,
				MessageType.INFO.getPopupBackground(),
				evt -> {
					if (HyperlinkEvent.EventType.ACTIVATED.equals(evt.getEventType())) {
						if (A_HREF_COPY.equals(evt.getDescription())) {
							final String info = createProductInfo().toString();
							CopyPasteManager.getInstance().setContents(new StringSelection(info));
						} else {
							BrowserUtil.browse(evt.getURL());
						}
					}
				}
		);
		builder.setHideOnClickOutside(true);
		builder.setHideOnKeyOutside(true);
		final Balloon balloon = builder.createBalloon();
		balloon.show(new RelativePoint(parent, new Point(parent.getWidth() / 2, parent.getHeight() / 2)), BalloonTipFactory.Orientation.RIGHT.getOrientation());
	}

	@NonNls
	@NotNull
	private static StringBuilder createHelpInfo() {
		final StringBuilder ret = new StringBuilder();
		ret.append("<p>");
		ret.append("<h2>").append(VersionManager.getFullVersion()).append("</h2>");
		ret.append("Website: <a href='").append(VersionManager.getWebsite()).append("'>").append(VersionManager.getWebsite()).append("</a>");
		ret.append("<br>");
		ret.append("Download: <a href='").append(VersionManager.getDownloadWebsite()).append("'>").append(VersionManager.getDownloadWebsite()).append("</a>");
		ret.append("<br>");
		ret.append("Issue tracker: ").append(VersionManager.getIssueTracker());
		ret.append("</p>");
		ret.append("<p>");
		ret.append("<h3>Findbugs ").append(FindBugsUtil.getFindBugsFullVersion()).append("</h3>");
		ret.append("Website: <a href='").append(Version.WEBSITE).append("'>").append(Version.WEBSITE).append("</a>");
		ret.append("<br>");
		ret.append("Download: <a href='").append(DOWNLOADS_WEBSITE).append("'>").append(DOWNLOADS_WEBSITE).append("</a>");
		ret.append("</p>");
		ret.append("<p>");
		ret.append("The name FindBugs&trade; and the FindBugs logo are trademarked by The University of Maryland.");
		ret.append("</p>");
		ret.append("<p>");
		ret.append("<a href='").append(A_HREF_COPY).append("'>").append(ResourcesLoader.getString("help.copyInfos")).append("</a>");
		ret.append("</p>");
		return ret;
	}

	/**
	 * Based on com.intellij.ide.actions.AboutPopup
	 */
	@NonNls
	@NotNull
	public static StringBuilder createProductInfo() {

		final StringBuilder ret = new StringBuilder("\n");
		ret.append("Product Infos");
		ret.append("\n    FindBugs: ").append(FindBugsUtil.getFindBugsFullVersion());
		ret.append("\n    FindBugs-IDEA: ").append(VersionManager.getFullVersion());

		boolean ideaVersionAvailable = false;
		try {
			final ApplicationInfoEx appInfo = ApplicationInfoEx.getInstanceEx();
			if (appInfo != null) {
				ret.append("\n    IDEA: ").append(appInfo.getFullApplicationName());
				ret.append("\n    IDEA-Build: ").append(appInfo.getBuild().asString());
				final Calendar cal = appInfo.getBuildDate();
				ret.append(", ").append(DateFormatUtil.formatAboutDialogDate(cal.getTime()));
				if (appInfo.getBuild().isSnapshot()) {
					ret.append(" ").append(new SimpleDateFormat("HH:mm, ").format(cal.getTime()));
				}
				ideaVersionAvailable = true;
			}
		} catch (final Throwable e) { // maybe ApplicationInfoEx API changed
			e.printStackTrace();
		}
		if (!ideaVersionAvailable) {
			ret.append("\n    IDEA: [Please type IDEA version here]");
		}

		final Properties systemProps = System.getProperties();
		final String javaVersion = systemProps.getProperty("java.runtime.version", systemProps.getProperty("java.version", "unknown"));
		final String arch = systemProps.getProperty("os.arch", "");
		ret.append("\n    JRE: ").append(javaVersion).append(" ").append(arch);

		final String vmVersion = systemProps.getProperty("java.vm.name", "unknown");
		final String vmVendor = systemProps.getProperty("java.vendor", "unknown");
		ret.append("\n    JVM: ").append(vmVersion).append(" ").append(vmVendor);

		return ret;
	}
}
