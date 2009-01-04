
package net.sourceforge.filebot.ui.panel.episodelist;


import static net.sourceforge.filebot.ui.panel.episodelist.SeasonSpinnerModel.ALL_SEASONS;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;

import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.ui.AbstractSearchPanel;
import net.sourceforge.filebot.ui.FileBotList;
import net.sourceforge.filebot.ui.FileBotListExportHandler;
import net.sourceforge.filebot.ui.FileBotTab;
import net.sourceforge.filebot.ui.SelectDialog;
import net.sourceforge.filebot.ui.transfer.FileExportHandler;
import net.sourceforge.filebot.ui.transfer.SaveAction;
import net.sourceforge.filebot.web.AnidbClient;
import net.sourceforge.filebot.web.Episode;
import net.sourceforge.filebot.web.EpisodeListClient;
import net.sourceforge.filebot.web.SearchResult;
import net.sourceforge.filebot.web.TVDotComClient;
import net.sourceforge.filebot.web.TVRageClient;
import net.sourceforge.filebot.web.TheTVDBClient;
import net.sourceforge.tuned.ui.LabelProvider;
import net.sourceforge.tuned.ui.SelectButton;
import net.sourceforge.tuned.ui.SimpleLabelProvider;
import net.sourceforge.tuned.ui.TunedUtil;


public class EpisodeListPanel extends AbstractSearchPanel<EpisodeListClient, Episode> {
	
	private SeasonSpinnerModel seasonSpinnerModel = new SeasonSpinnerModel();
	
	
	public EpisodeListPanel() {
		super("Episodes", ResourceManager.getIcon("panel.episodelist"));
		
		historyPanel.setColumnHeader(0, "Show");
		historyPanel.setColumnHeader(1, "Number of Episodes");
		
		JSpinner seasonSpinner = new JSpinner(seasonSpinnerModel);
		seasonSpinner.setEditor(new SeasonSpinnerEditor(seasonSpinner));
		
		// set minimum size to "All Seasons" preferred size
		seasonSpinner.setMinimumSize(seasonSpinner.getPreferredSize());
		
		// add after text field
		add(seasonSpinner, 1);
		// add after tabbed pane
		tabbedPaneGroup.add(new JButton(new SaveAction(new SelectedTabExportHandler())), "align center, wrap 5px");
		
		searchTextField.getSelectButton().addPropertyChangeListener(SelectButton.SELECTED_VALUE, selectButtonListener);
		
		TunedUtil.putActionForKeystroke(this, KeyStroke.getKeyStroke("shift UP"), new SpinSeasonAction(1));
		TunedUtil.putActionForKeystroke(this, KeyStroke.getKeyStroke("shift DOWN"), new SpinSeasonAction(-1));
	}
	

	@Override
	protected List<EpisodeListClient> createSearchEngines() {
		List<EpisodeListClient> engines = new ArrayList<EpisodeListClient>(3);
		
		engines.add(new TVRageClient());
		engines.add(new AnidbClient());
		engines.add(new TheTVDBClient("58B4AA94C59AD656"));
		engines.add(new TVDotComClient());
		
		return engines;
	}
	

	@Override
	protected LabelProvider<EpisodeListClient> createSearchEngineLabelProvider() {
		return SimpleLabelProvider.forClass(EpisodeListClient.class);
	}
	

	@Override
	protected EpisodeListRequestProcessor createRequestProcessor() {
		EpisodeListClient client = searchTextField.getSelectButton().getSelectedValue();
		String text = searchTextField.getText().trim();
		int season = seasonSpinnerModel.getSeason();
		
		return new EpisodeListRequestProcessor(new EpisodeListRequest(client, text, season));
	};
	
	private final PropertyChangeListener selectButtonListener = new PropertyChangeListener() {
		
		public void propertyChange(PropertyChangeEvent evt) {
			EpisodeListClient client = searchTextField.getSelectButton().getSelectedValue();
			
			// lock season spinner on "All Seasons" if client doesn't support fetching of single seasons
			if (!client.hasSingleSeasonSupport()) {
				seasonSpinnerModel.lock(ALL_SEASONS);
			} else {
				seasonSpinnerModel.unlock();
			}
		}
	};
	
	
	private class SpinSeasonAction extends AbstractAction {
		
		public SpinSeasonAction(int spin) {
			putValue("spin", spin);
		}
		

		public void actionPerformed(ActionEvent e) {
			seasonSpinnerModel.spin((Integer) getValue("spin"));
		}
	}
	

	private class SelectedTabExportHandler implements FileExportHandler {
		
		/**
		 * @return the <code>FileExportHandler</code> of the currently selected tab
		 */
		@SuppressWarnings("unchecked")
		private FileExportHandler getExportHandler() {
			try {
				EpisodeListTab list = ((FileBotTab<EpisodeListTab>) tabbedPane.getSelectedComponent()).getComponent();
				return list.getExportHandler();
			} catch (ClassCastException e) {
				// selected component is the history panel
				return null;
			}
		}
		

		@Override
		public boolean canExport() {
			FileExportHandler handler = getExportHandler();
			
			if (handler == null)
				return false;
			
			return handler.canExport();
		}
		

		@Override
		public void export(File file) throws IOException {
			getExportHandler().export(file);
		}
		

		@Override
		public String getDefaultFileName() {
			return getExportHandler().getDefaultFileName();
		}
		
	}
	

	protected static class EpisodeListRequest extends Request {
		
		private final EpisodeListClient client;
		private final int season;
		
		
		public EpisodeListRequest(EpisodeListClient client, String searchText, int season) {
			super(searchText);
			this.client = client;
			this.season = season;
		}
		

		public EpisodeListClient getClient() {
			return client;
		}
		

		public int getSeason() {
			return season;
		}
		
	}
	

	protected static class EpisodeListRequestProcessor extends RequestProcessor<EpisodeListRequest, Episode> {
		
		public EpisodeListRequestProcessor(EpisodeListRequest request) {
			super(request, new EpisodeListTab());
		}
		

		@Override
		public Collection<SearchResult> search() throws Exception {
			return request.getClient().search(request.getSearchText());
		}
		

		@Override
		public Collection<Episode> fetch() throws Exception {
			Collection<Episode> episodes;
			
			if (request.getSeason() != ALL_SEASONS) {
				episodes = request.getClient().getEpisodeList(getSearchResult(), request.getSeason());
			} else {
				episodes = request.getClient().getEpisodeList(getSearchResult());
			}
			
			// find max. episode number string length
			int maxLength = 1;
			
			for (Episode episode : episodes) {
				String num = episode.getEpisodeNumber();
				
				if (num.matches("\\d+") && num.length() > maxLength) {
					maxLength = num.length();
				}
			}
			
			// pad episode numbers with zeros (e.g. %02d) so all episode numbers have the same number of digits
			String format = "%0" + maxLength + "d";
			for (Episode episode : episodes) {
				
				try {
					episode.setEpisodeNumber(String.format(format, Integer.parseInt(episode.getEpisodeNumber())));
				} catch (NumberFormatException e) {
					// ignore
				}
			}
			
			return episodes;
		}
		

		@Override
		public URI getLink() {
			if (request.getSeason() != ALL_SEASONS)
				return request.getClient().getEpisodeListLink(getSearchResult(), request.getSeason());
			else
				return request.getClient().getEpisodeListLink(getSearchResult());
		}
		

		@Override
		public void process(Collection<Episode> episodes) {
			// set a proper title for the export handler before adding episodes
			getComponent().setTitle(getTitle());
			
			getComponent().getModel().addAll(episodes);
		}
		

		@Override
		public String getStatusMessage(Collection<Episode> result) {
			return (result.isEmpty()) ? "No episodes found" : String.format("%d episodes", result.size());
		}
		

		@Override
		public EpisodeListTab getComponent() {
			return (EpisodeListTab) super.getComponent();
		}
		

		@Override
		public String getTitle() {
			if (request.getSeason() == ALL_SEASONS)
				return super.getTitle();
			
			// add additional information to default title
			return String.format("%s - Season %d", super.getTitle(), request.getSeason());
		}
		

		@Override
		public Icon getIcon() {
			return request.getClient().getIcon();
		}
		

		@Override
		protected void configureSelectDialog(SelectDialog<SearchResult> selectDialog) {
			super.configureSelectDialog(selectDialog);
			selectDialog.getHeaderLabel().setText("Select a Show:");
		}
		
	}
	

	protected static class EpisodeListTab extends FileBotList<Episode> {
		
		public EpisodeListTab() {
			// set export handler for episode list
			setExportHandler(new FileBotListExportHandler(this));
			
			// allow removal of episode list entries
			getRemoveAction().setEnabled(true);
			
			// remove borders
			listScrollPane.setBorder(null);
			setBorder(null);
		}
		
	}
	
}
