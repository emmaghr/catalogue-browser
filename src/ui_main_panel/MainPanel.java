package ui_main_panel;

import java.util.Observable;
import java.util.Observer;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import catalogue.Catalogue;
import catalogue_object.Hierarchy;
import catalogue_object.Nameable;
import catalogue_object.Term;
import data_collection.DCTableConfig;
import dcf_log.DcfResponse;
import dcf_pending_request.PendingRequestActionsListener;
import global_manager.GlobalManager;
import messages.Messages;
import pending_request.IPendingRequest;
import pending_request.PendingRequestStatus;
import pending_request.PendingRequestStatusChangedEvent;
import sas_remote_procedures.XmlChangesService;
import session_manager.BrowserWindowPreferenceDao;
import soap.UploadCatalogueFileImpl;
import soap.UploadCatalogueFileImpl.PublishLevel;
import soap.UploadCatalogueFileImpl.ReserveLevel;
import ui_console.ConsoleMessage;
import ui_console.ConsoleMessageFactory;
import ui_dcf_log.LogNodesForm;
import ui_main_menu.FileActions;
import ui_main_menu.FileMenu;
import ui_main_menu.LoginMenu;
import ui_main_menu.MainMenu;
import ui_main_menu.MenuListener;
import ui_main_menu.ToolsMenu;
import ui_main_menu.ViewMenu;
import ui_main_panel.IBrowserPendingRequestWorker.PendingRequestWorkerListener;
import ui_search_bar.HierarchyChangedListener;
import ui_search_bar.HierarchyEvent;
import ui_search_bar.SearchEvent;
import ui_search_bar.SearchListener;
import ui_search_bar.SearchPanel;
import ui_user_console.UserConsoleDialog;
import user_preferences.CataloguePreferenceDAO;
import user_preferences.GlobalPreference;
import user_preferences.GlobalPreferenceDAO;
import user_preferences.PreferenceNotFoundException;
import utilities.GlobalUtil;
import window_restorer.RestoreableWindow;

/**
 * Main UI class, it displays the main page of the browser. Here we have the
 * main tree viewer, the term properties ( names, scopenotes, implicit
 * attributes, implicit facets, applicabilities...), a search bar, a combo box
 * to select the current hierarchy or facet list...
 * 
 * @author Thomas Milani(documentation), Valentino Avon
 * @version 0.2.1
 */
public class MainPanel implements Observer {

	private static final Logger LOGGER = LogManager.getLogger(MainPanel.class);

	// code for saving window dimensions in db
	private RestoreableWindow window;
	private final static String WINDOW_CODE = "MainPanel";

	// the shell which hosts the UI
	public Shell shell;

	// main menu (upper left menu)
	private MainMenu menu;

	// search bar and table
	private SearchPanel searchPanel;

	// label which shows the current open catalogue label
	private CatalogueLabel catalogueLabel;

	// combo box with radio buttons to select the displayed hierarchy
	private HierarchySelector hierarchySelector;

	// checkboxes to filter the tree terms
	private TermFilter termFilter;

	// main tree which allows browsing catalogue terms
	private TermsTreePanel tree;

	// term properties in three tabs
	private TermPropertiesPanel tabPanel;

	private UserConsoleDialog userConsole;

	public MainPanel(Shell shell, IBrowserPendingRequestWorker requestWorker) {
		this.shell = shell;
		listenPendingRequests(requestWorker);
	}

	public void addMessageToConsole(ConsoleMessage message) {
		userConsole.selectTab(0);
		userConsole.setVisible(true);
		userConsole.add(message);
	}

	public void addRequestToConsole(IPendingRequest request) {
		userConsole.add(request);
	}

	public void openUserConsole() {
		userConsole.setVisible(true);
	}

	private void listenPendingRequests(IBrowserPendingRequestWorker worker) {

		worker.addActionListener(new PendingRequestActionsListener() {
			@Override
			public void actionPerformed(PendingRequestActionsEvent event) {

				shell.getDisplay().asyncExec(new Runnable() {

					@Override
					public void run() {

						ActionPerformed action = event.getAction();

						String catCode = event.getCatalogueCode();
						String oldVersion = event.getOldVersion();
						String version = event.getVersion();
						String livVersion = event.getLastInternalVersion();

						String text = null;
						int colour = SWT.COLOR_GREEN;

						switch (action) {
						case LIV_IMPORT_STARTED:
							text = Messages.getString("liv.downloading", catCode);
							colour = SWT.COLOR_DARK_GREEN;
							break;
						case LIV_IMPORTED:
							text = Messages.getString("liv.imported", catCode, livVersion);
							colour = SWT.COLOR_DARK_GREEN;
							break;
						case TEMP_CAT_CONFIRMED:
							text = Messages.getString("temp.confirmed", catCode, oldVersion, version);
							colour = SWT.COLOR_DARK_GREEN;
							break;
						case TEMP_CAT_CREATED:
							text = Messages.getString("temp.created", catCode, oldVersion, version);
							colour = SWT.COLOR_DARK_YELLOW;
							break;
						case TEMP_CAT_INVALIDATED_LIV:
							text = Messages.getString("temp.inv.liv", catCode, oldVersion, version);
							colour = SWT.COLOR_DARK_RED;
							break;
						case TEMP_CAT_INVALIDATED_NO_RESERVE:
							text = Messages.getString("temp.inv.reserve", catCode, oldVersion, version);
							colour = SWT.COLOR_DARK_RED;
							break;
						case NEW_INTERNAL_VERSION_CREATED:
							text = Messages.getString("new.internal.version", catCode, oldVersion, version);
							colour = SWT.COLOR_DARK_GREEN;
							break;
						default:
							break;
						}

						addMessageToConsole(new ConsoleMessage(text, colour));
					}
				});
			}
		});

		worker.addListener(new PendingRequestWorkerListener() {

			@Override
			public void workerStatusChanged(final WorkerStatus newStatus) {

				shell.getDisplay().asyncExec(new Runnable() {

					@Override
					public void run() {
						if (newStatus == WorkerStatus.ONGOING) {
							ShellLocker.setLock(shell, Messages.getString("MainPanel.CannotCloseTitle"),
									Messages.getString("MainPanel.CannotCloseMessage"));
						} else {
							ShellLocker.removeLock(shell);
						}
					}
				});
			}

			@Override
			public void statusChanged(final PendingRequestStatusChangedEvent event) {

				LOGGER.debug("Received pending request event=" + event);

				shell.getDisplay().asyncExec(new Runnable() {

					@Override
					public void run() {

						// if a new request started, add the request to the user console
						if (event.getOldStatus() == PendingRequestStatus.WAITING)
							addRequestToConsole(event.getPendingRequest());
						else
							userConsole.refresh(event.getPendingRequest());
					}
				});

				// get the code of the catalogue involved
				// in the pending request

				String catalogueCode = event.getPendingRequest().getData()
						.get(UploadCatalogueFileImpl.CATALOGUE_CODE_DATA_KEY);

				switch (event.getNewStatus()) {
				case ERROR:

					shell.getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {

							ConsoleMessage message = new ConsoleMessage(event.getPendingRequest().getType() + " "
									+ catalogueCode + " " + Messages.getString("pending.request.error"), SWT.COLOR_RED);

							addMessageToConsole(message);
						}
					});

					break;

				case QUEUED:

					shell.getDisplay().asyncExec(new Runnable() {

						@Override
						public void run() {
							ConsoleMessageFactory factory = new ConsoleMessageFactory(catalogueCode);
							ConsoleMessage message = null;
							switch (event.getPendingRequest().getType()) {
							case IPendingRequest.TYPE_PUBLISH_MAJOR:
								message = factory.getQueuedPublishMessage(PublishLevel.MAJOR);
								break;
							case IPendingRequest.TYPE_PUBLISH_MINOR:
								message = factory.getQueuedPublishMessage(PublishLevel.MINOR);
								break;
							case IPendingRequest.TYPE_RESERVE_MAJOR:
								message = factory.getQueuedReserveMessage(ReserveLevel.MAJOR);
								break;
							case IPendingRequest.TYPE_RESERVE_MINOR:
								message = factory.getQueuedReserveMessage(ReserveLevel.MINOR);
								break;
							case IPendingRequest.TYPE_UNRESERVE:
								message = factory.getQueuedUnreserveMessage();
								break;
							case XmlChangesService.TYPE_UPLOAD_XML_DATA:
								message = factory.getQueuedXmlDataMessage();
								break;
							}

							addMessageToConsole(message);
						}
					});

					break;

				case COMPLETED:

					shell.getDisplay().asyncExec(new Runnable() {

						@Override
						public void run() {

							final DcfResponse response = event.getPendingRequest().getResponse();

							if (response == DcfResponse.OK) {
								refresh();
							} else {

								if (event.getPendingRequest().getLog() != null) {

									// if there are errors, show also the errors table
									LogNodesForm errors = new LogNodesForm(shell, event.getPendingRequest().getLog());
									errors.open();
								}
							}

							ConsoleMessageFactory factory = new ConsoleMessageFactory(catalogueCode);

							ConsoleMessage message = null;
							switch (event.getPendingRequest().getType()) {
							case IPendingRequest.TYPE_PUBLISH_MAJOR:
								message = factory.getPublishCompletedMessage(response, PublishLevel.MAJOR);
								break;
							case IPendingRequest.TYPE_PUBLISH_MINOR:
								message = factory.getPublishCompletedMessage(response, PublishLevel.MINOR);
								break;
							case IPendingRequest.TYPE_RESERVE_MAJOR:
								message = factory.getReserveCompletedMessage(response, ReserveLevel.MAJOR);
								break;
							case IPendingRequest.TYPE_RESERVE_MINOR:
								message = factory.getReserveCompletedMessage(response, ReserveLevel.MINOR);
								break;
							case IPendingRequest.TYPE_UNRESERVE:
								message = factory.getUnreserveCompletedMessage(response);
								break;
							case XmlChangesService.TYPE_UPLOAD_XML_DATA:
								message = factory.getXmlDataCompletedMessage(response);
								break;
							}

							addMessageToConsole(message);
						}
					});
					break;
				default:
					break;
				}
			}
		});
	}

	/**
	 * Initialize the main UI panel
	 * 
	 * @param shell
	 */
	public MainPanel(final Shell shell) {
		this(shell, BrowserPendingRequestWorker.getInstance());
	}

	public MainMenu getMenu() {
		return menu;
	}

	/**
	 * Creates the user interface
	 */
	public void initGraphics() {

		// add the main menu to the shell
		addMainMenu(shell);

		// add all the swt widgets to the main UI
		addWidgets(shell);

		// shell name, image, window dimensions (based on
		// widget! Need to call it after addWidgets)
		setShellGraphics();
	}


	public void openLastCatalogue() {
		// open the last catalogue if present
		try {
			Catalogue catalogue = getLastOpenedCatalogue();

			if (catalogue == null)
				return;

			catalogue.open();

			// enable the user interface only if we have data in the current catalogue
			if (!catalogue.isEmpty()) {
				enableUI(true);
				loadData(catalogue);
			}
		} catch (PreferenceNotFoundException e) {
		}
	}

	/**
	 * Refresh the catalogue of the objects
	 * 
	 * @param catalogue
	 */
	public void refresh(final Catalogue catalogue) {

		// redraw menus in the ui thread ( we use async exec since
		// this method is potentially called by threads not in the UI )
		Display.getDefault().asyncExec(new Runnable() {

			@Override
			public void run() {

				catalogueLabel.setText(catalogue);
				menu.setCatalogue(catalogue);

				refresh();

				Display.getDefault().update();

				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
					LOGGER.error("Cannot stop thread", e);
				}
			}
		});
	}

	/**
	 * Method for Refreshing GUI. Refresh Search, tree and(in case of not ReadOnly)
	 * corex flag and state flag; folder and shell redraw; shell update and layout
	 */
	public void refresh() {

		// redraw menus in the ui thread ( we use async exec since
		// this method is potentially called by threads not in the UI )
		Display.getDefault().asyncExec(new Runnable() {

			@Override
			public void run() {

				if (shell.isDisposed())
					return;

				if (shell.getMenu() != null)
					shell.getMenu().dispose();

				// redraw the main menu to refresh buttons
				shell.setMenuBar(menu.createMainMenu());
				menu.refresh();

				// redraw the tree menu to refresh buttons
				tree.addContextualMenu(true);

				tabPanel.refresh();
				tabPanel.redraw();

				catalogueLabel.refresh();

				hierarchySelector.refresh();

				searchPanel.refresh(true);
				// tree.refresh( true );

				shell.redraw();
				shell.update();
				shell.layout();

				Display.getDefault().update();

				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
					LOGGER.error("Cannot sleep thread", e);
				}
			}
		});
	}

	/**
	 * Get the last opened catalogue if there is one
	 * 
	 * @return
	 * @throws PreferenceNotFoundException
	 */
	private Catalogue getLastOpenedCatalogue() throws PreferenceNotFoundException {
		// save main panel state
		GlobalPreferenceDAO prefDao = new GlobalPreferenceDAO();
		return prefDao.getLastCatalogue();
	}

	/**
	 * Save the state of the main panel for the current catalogue (selected
	 * hierarchy and selected term!)
	 */
	private void saveState() {

		Catalogue current = GlobalManager.getInstance().getCurrentCatalogue();

		// no catalogue opened, return
		if (current == null)
			return;

		// save main panel state
		CataloguePreferenceDAO prefDao = new CataloguePreferenceDAO(current);

		prefDao.saveMainPanelState(hierarchySelector.getSelectedHierarchy(), tree.getFirstSelectedTerm());
	}

	/**
	 * Get the last hierarchy if present
	 * 
	 * @param catalogue
	 * @return
	 * @throws PreferenceNotFoundException
	 */
	private Hierarchy getLastHierarchy(Catalogue catalogue) throws PreferenceNotFoundException {

		CataloguePreferenceDAO prefDao = new CataloguePreferenceDAO(catalogue);

		Hierarchy lastHierarchy = null;

		// first try to load the last hierarchy
		lastHierarchy = prefDao.getLastHierarchy();

		return lastHierarchy;
	}

	/**
	 * Get the last term if present
	 * 
	 * @param catalogue
	 * @return
	 * @throws PreferenceNotFoundException
	 */
	private Term getLastTerm(Catalogue catalogue) throws PreferenceNotFoundException {

		CataloguePreferenceDAO prefDao = new CataloguePreferenceDAO(catalogue);

		Term lastTerm = null;

		// first try to load the last hierarchy
		lastTerm = prefDao.getLastTerm();

		return lastTerm;
	}

	/**
	 * Set the name and the image of the shell. Moreover, set the layout and the
	 * paint listener
	 */
	private void setShellGraphics() {

		window = new RestoreableWindow(shell, WINDOW_CODE);

		/* use layout manager */
		shell.setLayout(new GridLayout(1, false));

		shell.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				shell.layout();
			}
		});

		shell.setMaximized(true);

		// if shell disposed
		shell.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent arg0) {
				saveState();
			}
		});

		// restore the old dimensions of the window
		window.restore(BrowserWindowPreferenceDao.class);

		// save this window dimensions when it is closed
		window.saveOnClosure(BrowserWindowPreferenceDao.class);
	}

	/**
	 * Change the hierarchy to be visualized and expand the tree to the selected
	 * term
	 * 
	 * @param hierarchy
	 *            the new hierarchy to be visualized
	 * @param term
	 *            the selected term which has to be reopened in the new hierarchy
	 */
	public void changeHierarchy(Hierarchy hierarchy, Nameable term) {

		changeHierarchy(hierarchy);

		// select the term in the tree
		tree.selectTerm(term, true);

		// refresh the browser
		refresh();
	}

	/**
	 * Change the hierarchy to be visualized
	 * 
	 * @param hierarchy
	 *            the new hierarchy to be visualized
	 */
	public void changeHierarchy(Hierarchy hierarchy) {
		// update the combo box selection
		hierarchySelector.setSelection(hierarchy);
	}

	/**
	 * Enable or disable the entire user interface
	 */
	private void enableUI(boolean enable) {

		// enable the combo box for hierarchies
		hierarchySelector.setEnabled(enable);

		// enable search bar
		searchPanel.setEnabled(enable);

		// enable display filters
		termFilter.setEnabled(enable);
	}

	/**
	 * Load all the default graphics input and listeners
	 */
	private void loadData(Catalogue catalogue) {

		// update the tree input and set its contextual menu
		// set the tree input

		tree.addUpdateListener(new Listener() {

			@Override
			public void handleEvent(Event event) {
				tabPanel.refresh();
			}
		});

		// refresh applicabilities when drop finishes
		tree.addDropListener(new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				tabPanel.refresh();
			}
		});

		// if "see in other hierarchies" was pressed
		tree.addChangeHierarchyListener(new HierarchyChangedListener() {

			@Override
			public void hierarchyChanged(HierarchyEvent event) {

				// update the hierarchy to be shown in the main pane
				changeHierarchy(event.getHierarchy(), (Term) event.getTerm());
			}
		});

		// AlbyDev: add the selected term to the list of indexes
		/*
		 * tree.addSelectionChangedListener(new ISelectionChangedListener() {
		 * 
		 * @Override public void selectionChanged(SelectionChangedEvent arg0) {
		 * TreeItemSelection sel = new TreeItemSelection(tree.getFirstSelectedTerm(),
		 * hierarchySelector.getSelectedHierarchy());
		 * navigationalButtons.addNewObject(sel); } });
		 * 
		 * navigationalButtons.addNavButtonsListener(new
		 * INavButtonsListener<TreeItemSelection>() {
		 * 
		 * @Override public void forwardPressed(TreeItemSelection sel) {
		 * changeHierarchy(sel.getHierarchy(), sel.getTerm()); }
		 * 
		 * @Override public void backPressed(TreeItemSelection sel) {
		 * changeHierarchy(sel.getHierarchy(), sel.getTerm()); } });
		 */

		// set the hierarchy combo box input and select the first available hierarchy
		hierarchySelector.refresh();

		Hierarchy hierarchy;
		try {

			// try to load the hierarchy selector state
			hierarchy = getLastHierarchy(catalogue);

		} catch (PreferenceNotFoundException e) {

			// set the first selection of the hierarchy selector
			// with the default hierarchy if no preference was found
			hierarchy = catalogue.getDefaultHierarchy();
		}

		// set the selection
		hierarchySelector.setSelection(hierarchy);

		// recover the last selected term if present
		try {
			Term lastTerm = getLastTerm(catalogue);
			tree.selectTerm(lastTerm, true);
		} catch (PreferenceNotFoundException e) {
		}

		// update also the tree input
		// tree.setInput( hierarchy );
	}

	/**
	 * Remove all the input from the graphics
	 */
	private void removeData() {

		// remove elements from the tree
		tree.setInput(null);

		// remove the term from the panel
		tabPanel.setTerm(null);

		// remove input from tabs
		tabPanel.resetInput();

		// disable tabs
		tabPanel.setEnabled(false);

		// disable search
		searchPanel.setEnabled(false);

		// clear search results
		searchPanel.removeAll();

		hierarchySelector.resetGraphics();
		hierarchySelector.setEnabled(false);
	}

	/**
	 * Add all the widgets to the main UI
	 * 
	 * @param parent
	 */
	private void addWidgets(Composite parent) {

		this.userConsole = new UserConsoleDialog(shell, SWT.RESIZE | SWT.DIALOG_TRIM);
		this.userConsole.setText(Messages.getString("user.console.title"));

		if (shell.getImage() != null)
			this.userConsole.setImage(shell.getImage());

		// do not close the console, just make it non visible
		this.userConsole.addCloseListener(new Listener() {

			@Override
			public void handleEvent(Event event) {
				event.doit = false;
				userConsole.setVisible(!userConsole.isVisible());
			}
		});

		// I add a sashForm which is a split pane
		SashForm sashForm = new SashForm(shell, SWT.HORIZONTAL);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		// left group for catalogue label, search bar and table
		GridData leftData = new GridData();
		leftData.minimumWidth = 180;
		leftData.widthHint = 180;
		
		Composite leftGroup = new Composite(sashForm, SWT.NONE);
		leftGroup.setLayout(new GridLayout(1, false));
		leftGroup.setLayoutData(leftData);

		// add the label which displays the catalogue label
		addCatalogueLabel(leftGroup);

		// add the search bar and table
		addSearchPanel(leftGroup);
		
		// group which contains hierarchy selector, tree viewer and tab folder
		Group rightGroup = new Group(sashForm, SWT.NONE);
		rightGroup.setLayout(new GridLayout(1, false));

		GridData gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;

		rightGroup.setLayoutData(gridData);

		// add hierarchy selector and deprecated/non reportable filters
		addDisplayFilters(rightGroup);

		// add tree viewer and term tabs
		addRightSashForm(rightGroup);

		// make the tree viewer observer of the selected hierarchy
		hierarchySelector.addObserver(tree);

		// make implicit facet tab observer of selected hierarchy
		hierarchySelector.addObserver(tabPanel);

		// make the search results table aware of the current hierarchy
		hierarchySelector.addObserver(searchPanel);

		// make the tree observer of the checked filters
		termFilter.addObserver(tree);

		// objects which observe global manager current catalogue
		GlobalManager.getInstance().addObserver(hierarchySelector);
		GlobalManager.getInstance().addObserver(searchPanel);
		GlobalManager.getInstance().addObserver(tree);
		GlobalManager.getInstance().addObserver(catalogueLabel);
		GlobalManager.getInstance().addObserver(menu);
		GlobalManager.getInstance().addObserver(tabPanel);
		GlobalManager.getInstance().addObserver(this);

		// tab panel listen term changes
		tree.addObserver(tabPanel);

		// add the tree as observer of the term filter
		// and restore the term filter status to the
		// previous one
		termFilter.addObserver(tree);
		termFilter.addObserver(searchPanel);
		termFilter.restoreStatus();

		// set the weights once all the widgets are inserted
		// AlbyDev: increased the width for the search view (second parm from 4 to 3)
		sashForm.setWeights(new int[] { 1, 3 });
	}

	/**
	 * Add the main menu to the shell
	 * 
	 * @param shell
	 */
	private void addMainMenu(final Shell shell) {

		// create the main menu and set its listener for some buttons
		menu = new MainMenu(shell, this);

		menu.setFileListener(new MenuListener() {

			@Override
			public void buttonPressed(MenuItem button, int code, Event event) {

				switch (code) {

				case FileMenu.OPEN_DC_MI:

					// get the selected hierarchy
					DCTableConfig config = (DCTableConfig) event.data;

					Catalogue targetCat = config.getCatalogue();
					Hierarchy hierarchy = config.getHierarchy();

					if (targetCat == null) {
						GlobalUtil.showErrorDialog(shell, config.getConfig().getCatalogueCode(),
								Messages.getString("FormConfigList.CatNotPresentError"));
						return;
					}

					if (hierarchy == null) {
						GlobalUtil.showErrorDialog(shell,
								config.getConfig().getCatalogueCode() + " - " + config.getConfig().getHierarchyCode(),
								Messages.getString("FormConfigList.HierNotPresentError"));
						return;
					}

					// warn user if necessary
					int ok = FileActions.openCatalogue(shell, targetCat);

					if (ok != SWT.YES)
						break;

					// enable the user interface only if
					// we have data in the current catalogue
					if (!targetCat.isEmpty()) {
						enableUI(true);
						loadData(targetCat);
					}

					// change the hierarchy to the target one
					changeHierarchy(hierarchy);

					break;

				case FileMenu.OPEN_CAT_MI:

					removeData();
					
					refresh();

					// get the selected catalogue
					Catalogue catalogue = (Catalogue) event.data;

					// enable the user interface only if we have data in the current catalogue
					if (!catalogue.isEmpty()) {
						enableUI(true);
						loadData(catalogue);
					}

					break;

				case FileMenu.CLOSE_CAT_MI:

					// save the current state for the current catalogue
					saveState();
					
					removeData();
					enableUI(false);

					refresh();

					break;

				default:
					break;
				}
			}
		});

		menu.setViewListener(new MenuListener() {

			@Override
			public void buttonPressed(MenuItem button, int code, Event event) {

				switch (code) {
				case ViewMenu.EXPAND_MI:
					tree.expandSelectedTerms(TreeViewer.ALL_LEVELS);
					break;
				case ViewMenu.COLLAPSE_NODE_MI:
					tree.collapseSelectedTerms(TreeViewer.ALL_LEVELS);
					break;
				case ViewMenu.COLLAPSE_TREE_MI:
					tree.collapseAll();
					break;
				default:
					break;
				}
			}
		});

		menu.setToolsListener(new MenuListener() {

			@Override
			public void buttonPressed(MenuItem button, int code, Event event) {

				switch (code) {

				case ToolsMenu.IMPORT_CAT_MI:

					// remove all the search table results
					// since we change the content of the catalogue
					searchPanel.removeAll();

					// get the current catalogue
					Catalogue catalogue = GlobalManager.getInstance().getCurrentCatalogue();

					// enable user interface if the catalogue is not empty
					if (catalogue != null && !catalogue.isEmpty()) {

						enableUI(true);

						// open the master hierarchy of the catalogue
						changeHierarchy(catalogue.getMasterHierarchy());
					}

					break;

					
				case ToolsMenu.DOWNLOADICT:
					//refresh the tools item menu when ict download and install is completed
					refresh();
					break;
					
				case ToolsMenu.HIER_EDITOR_MI:
				case ToolsMenu.ATTR_EDITOR_MI:
					// refresh
					tabPanel.setTerm(tree.getFirstSelectedTerm());
					refresh();
					break;
					
				default:
					break;
				}
			}
		});

		menu.setLoginListener(new MenuListener() {

			@Override
			public void buttonPressed(MenuItem button, int code, Event event) {
				switch (code) {
				case LoginMenu.LOGIN_MI:
					refresh();
					hierarchySelector.refreshFilter();
					break;
				default:
					break;
				}
			}
		});

		if (shell.getMenu() != null)
			shell.getMenu().dispose();

		// initialize the main menu with all the sub menus and menu items
		shell.setMenuBar(menu.createMainMenu());

		// set the main panel as observer of the main menu
		menu.addObserver(this);
	}

	/**
	 * Add a label which displays the current opened catalogue
	 * 
	 * @param parent
	 */
	private void addCatalogueLabel(Composite parent) {
		catalogueLabel = new CatalogueLabel(parent);
	}

	/**
	 * Add the search panel (search bar and search table results)
	 * 
	 * @param parent
	 */
	private void addSearchPanel(Composite parent) {

		// get the current catalogue
		Catalogue catalogue = GlobalManager.getInstance().getCurrentCatalogue();

		// add a search table
		searchPanel = new SearchPanel(parent, true, catalogue);
		
		// called when a hierarchy is selected in the
		// results table using the contextual menu
		searchPanel.addHierarchyListener(new HierarchyChangedListener() {
			@Override
			public void hierarchyChanged(HierarchyEvent event) {
				changeHierarchy(event.getHierarchy(), event.getTerm());
			}
		});

		// Set the search listener (actions performed at the end of the search)
		searchPanel.addSearchListener(new SearchListener() {

			@Override
			public void searchPerformed(SearchEvent event) {

				// if empty warn the user
				if (event.getResults().isEmpty()) {

					GlobalUtil.showDialog(shell, Messages.getString("Browser.SearchResultTitle"),
							Messages.getString("Browser.SearchResultMessage"), SWT.OK);
				}
			}
		});

		// expand the selected term of the search results in the tree if clicked
		searchPanel.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {

				IStructuredSelection selection = (IStructuredSelection) event.getSelection();

				// return if no selected elements
				if (selection.isEmpty())
					return;

				tree.selectTerm((Nameable) selection.getFirstElement(), false);

			}
		});

		// set Go as default button
		searchPanel.addDefaultButton(shell);
	}

	/**
	 * Add the hierarchy selector and check buttons to filter the tree viewer terms
	 * 
	 * @param parent
	 */
	private void addDisplayFilters(Composite parent) {

		Composite selectionGroup = new Composite(parent, SWT.NONE);
		selectionGroup.setLayout(new GridLayout(10, false));

		// hierarchy selector (combo box + radio buttons)
		hierarchySelector = new HierarchySelector(selectionGroup);
		hierarchySelector.display();

		// create a filter to filter the tree terms
		// based on their state (deprecated, not reportable)
		termFilter = new TermFilter(selectionGroup);
		termFilter.display(GlobalPreference.HIDE_DEPR_MAIN, GlobalPreference.HIDE_NOT_REP_MAIN,
				GlobalPreference.HIDE_TERM_CODE_MAIN);
	}

	/**
	 * Add the right sashform, that is, the form which contains the tree viewer and
	 * the tab folder.
	 * 
	 * @param parent
	 */
	private void addRightSashForm(Composite parent) {

		SashForm sashForm2 = new SashForm(parent, SWT.HORIZONTAL);

		GridData gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		sashForm2.setLayoutData(gridData);
		
		// get the current catalogue
		Catalogue catalogue = GlobalManager.getInstance().getCurrentCatalogue();

		// add the main tree viewer
		tree = new TermsTreePanel(sashForm2, catalogue);
		
		// add the tab folder
		addTabFolder(sashForm2);

		sashForm2.setWeights(new int[] { 5, 3 });
	}

	/**
	 * Add the tab folder
	 * 
	 * @param parent
	 */
	private void addTabFolder(Composite parent) {

		// get the current catalogue
		Catalogue catalogue = GlobalManager.getInstance().getCurrentCatalogue();

		// initialize tab panel
		tabPanel = new TermPropertiesPanel(parent, catalogue);

		// add the open listener, if we open an applicability
		// we move the hierarchy to the selected one
		tabPanel.addOpenListener(new HierarchyChangedListener() {

			@Override
			public void hierarchyChanged(HierarchyEvent event) {

				// get the selected hierarchy from the event
				Hierarchy selectedHierarchy = event.getHierarchy();
				Nameable parent = event.getTerm();

				// change the hierarchy, show term if term selected
				// otherwise just open the hierarchy
				if (parent instanceof Term)
					changeHierarchy(selectedHierarchy, parent);
				else
					changeHierarchy(selectedHierarchy);
			}
		});

		// set the add listener, if we add an applicability
		// refresh the implicit facet tab (the inherited facets changes)
		tabPanel.addAddListener(new Listener() {

			@Override
			public void handleEvent(Event event) {
				tree.refresh();
			}
		});

		// set the remove listener, if we remove an applicability
		// we refresh the tree and the implicit facet tab
		tabPanel.addRemoveListener(new Listener() {

			@Override
			public void handleEvent(Event event) {
				tree.refresh();
			}
		});

		// add the usage listener, if we change the usage we refresh the tree
		// since non reportable terms become italic
		tabPanel.addUsageListener(new HierarchyChangedListener() {

			@Override
			public void hierarchyChanged(HierarchyEvent event) {
				tree.refresh();
			}
		});

		// if an object is modified, then we refresh the tree
		tabPanel.addUpdateListener(new Listener() {

			@Override
			public void handleEvent(Event event) {
				Term term = (Term) event.data;
				tree.refresh(term);

				searchPanel.refresh(true);
			}
		});

	}

	// warned if the reserve level of the current catalogue is changed
	@Override
	public void update(Observable arg0, Object arg1) {

		// refresh ui if the current catalogue changed
		if (arg0 instanceof GlobalManager) {
			if (arg1 instanceof Catalogue) {
				refresh();
			}
		}

		else if (arg1 instanceof Catalogue) {
			refresh((Catalogue) arg1);
		}

		// refresh UI if the current catalogue reserve level was changed
		// or if refresh is required
		else if (arg1 instanceof ReserveLevel || arg0 instanceof MainMenu) {
			refresh();
		}
	}
}
