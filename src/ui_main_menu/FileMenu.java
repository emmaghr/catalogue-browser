package ui_main_menu;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import catalogue_browser_dao.CatalogueDAO;
import catalogue_generator.ThreadFinishedListener;
import data_collection.DCDAO;
import data_collection.DCTableConfig;
import dcf_manager.Dcf;
import dcf_user.User;
import messages.Messages;
import utilities.GlobalUtil;

/**
 * File menu for the main menu.
 * @author avonva
 *
 */
public class FileMenu implements MainMenuItem {

	// codes to identify the menu items (used for listeners)
	public static final int NEW_CAT_MI = 0;
	public static final int OPEN_CAT_MI = 1;
	public static final int OPEN_DC_MI = 7;
	public static final int IMPORT_CAT_MI = 2;
	public static final int DOWNLOAD_CAT_MI = 3;
	public static final int DOWNLOAD_DC_MI = 8;
	public static final int CLOSE_CAT_MI = 4;
	public static final int DELETE_CAT_MI = 5;
	public static final int EXIT_MI = 6;
	
	private MenuListener listener;
	
	private MainMenu mainMenu;
	private Shell shell;
	
	private MenuItem openMI;
	private MenuItem openDcMI;
	private MenuItem downloadDcMI;
	private MenuItem importCatMI;
	private MenuItem downloadMI;
	private MenuItem closeMI;
	private MenuItem deleteMI;
	/**
	 * Set the listener to the file menu
	 * @param listener
	 */
	public void setListener(MenuListener listener) {
		this.listener = listener;
	}
	
	/**
	 * Initialize the main file menu passing the main
	 * menu which contains it and the menu in which we
	 * want to create the file menu
	 * @param mainMenu
	 * @param menu
	 */
	public FileMenu( MainMenu mainMenu, Menu menu ) {
		this.mainMenu = mainMenu;
		this.shell = mainMenu.getShell();
		create( menu );
	}
	
	/**
	 * Create a file menu 
	 * @param menu
	 */
	public MenuItem create ( Menu menu ) {
		
		// create FILE Menu and its sub menu items
		Menu fileMenu = new Menu( menu );

		MenuItem fileItem = new MenuItem( menu , SWT.CASCADE );
		fileItem.setText( Messages.getString("BrowserMenu.FileMenuName") );
		fileItem.setMenu( fileMenu );
		
		addNewLocalCatMI ( fileMenu );

		openMI = addOpenDBMI ( fileMenu );  // open a catalogue
		
		openDcMI = addOpenDcMI ( fileMenu );
		
		importCatMI = addImportCatalogueMI ( fileMenu );
		
		downloadMI = addDownloadCatalogueMI ( fileMenu );  // download catalogue
		
		downloadDcMI = addDownloadDcMI ( fileMenu );
		
		closeMI = addCloseCatalogueMI ( fileMenu );
		
		deleteMI = addDeleteCatalogueMI ( fileMenu ); //  delete catalogue
		
		// add separator
		new MenuItem( fileMenu , SWT.SEPARATOR );
		
		addExitMI ( fileMenu );
		
		fileMenu.addListener( SWT.Show, new Listener() {
			
			@Override
			public void handleEvent(Event event) {
				refresh();
			}
		});
		
		return fileItem;
	}
	
	
	/**
	 * Add a "New..." item to the menu, 
	 * which allows to create a new local catalogue
	 * @param menu
	 */
	private MenuItem addNewLocalCatMI ( final Menu menu ) {

		final MenuItem newFileItem = new MenuItem( menu , SWT.NONE );
		newFileItem.setText( Messages.getString("BrowserMenu.NewCatalogueCmd") );
		
		// if the new local catalogue button is pressed
		newFileItem.addSelectionListener( new SelectionAdapter() {

			@Override
			public void widgetSelected ( SelectionEvent event ) {
				
				// create a new local catalogue
				FileActions.createNewLocalCatalogue( mainMenu.getShell() );
				
				if ( listener != null )
					listener.buttonPressed( newFileItem, NEW_CAT_MI, null );
			}
		} );
		
		return newFileItem;
	}
	

	/**
	 * Add a menu item which allows to download from the DCF a catalogue
	 * @param menu
	 * @return 
	 */
	private MenuItem addDownloadCatalogueMI ( Menu menu ) {
		
		// Item to see and load the catalogues directly from the DCF
		final MenuItem loadCatalogueItem = new MenuItem( menu, SWT.NONE );

		// if button pressed
		loadCatalogueItem.addSelectionListener( new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				FileActions.downloadCatalogue( shell );
				
				if ( listener != null )
					listener.buttonPressed( loadCatalogueItem, 
							DOWNLOAD_CAT_MI, null );
				
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		
		return loadCatalogueItem;
	}


	/**
	 * Add a menu item which allows to open a .catalog file which will be
	 * loaded as database
	 * @param menu
	 */
	private MenuItem addOpenDBMI ( Menu menu ) {
		
		final MenuItem openFileItem = new MenuItem( menu , SWT.NONE );
		openFileItem.setText( Messages.getString("BrowserMenu.OpenCatalogueCmd") );

		openFileItem.addSelectionListener( new SelectionAdapter() {

			@Override
			public void widgetSelected ( SelectionEvent event ) {
				
				FileActions.openCatalogue( shell, new Listener() {
					
					@Override
					public void handleEvent(Event arg0) {
						// refresh main menu
						mainMenu.refresh();
						
						if ( listener != null ) {
							listener.buttonPressed( openFileItem, OPEN_CAT_MI, arg0 );
						}
					}
				} );
			}
		} );
		
		return openFileItem;
	}
	
	/**
	 * Open data collection menu item
	 * @param menu
	 * @return
	 */
	private MenuItem addOpenDcMI ( Menu menu ) {
		
		final MenuItem openDc = new MenuItem( menu , SWT.NONE );
		openDc.setText( Messages.getString("BrowserMenu.OpenDcCmd") );

		openDc.addSelectionListener( new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				
				// open a data collection and possibly open
				// a configuration
				DCTableConfig config = FileActions.openDC( shell );
				
				if ( config == null )
					return;
				
				if ( listener != null ) {
					Event event = new Event();
					event.data = config;
					listener.buttonPressed( openDc, OPEN_DC_MI, event );
				}
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {}
		});
		
		return openDc;
	}
	
	/**
	 * 
	 * @param menu
	 */
	private MenuItem addDownloadDcMI ( Menu menu ) {
		
		final MenuItem downloadDc = new MenuItem( menu , SWT.NONE );
		downloadDc.setText( Messages.getString("BrowserMenu.DownloadDcCmd") );

		downloadDc.addSelectionListener( new SelectionAdapter() {

			@Override
			public void widgetSelected ( SelectionEvent event ) {
				FileActions.downloadDC( shell );
			}
		} );
		
		return downloadDc;
	}
	
	/**
	 * Add a menu item to import a catalogue database from a ecf file
	 * @param menu
	 * @return
	 */
	private MenuItem addImportCatalogueMI ( final Menu menu ) {
		
		final MenuItem importCatMI = new MenuItem( menu, SWT.PUSH );
		importCatMI.setText( Messages.getString( "BrowserMenu.ImportCatalogueCmd" ) );
		
		importCatMI.addSelectionListener( new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {

				if ( listener != null )
					listener.buttonPressed( importCatMI, 
							IMPORT_CAT_MI, null );
				
				FileActions.importCatalogue( shell, new ThreadFinishedListener() {
					
					@Override
					public void finished(Thread thread, final int code, Exception e) {
						
						// refresh menu items when the import is 
						// finished (needed to refresh open and delete buttons)
						mainMenu.refresh();
						
						mainMenu.getShell().getDisplay().asyncExec( new Runnable() {
							
							@Override
							public void run() {
								
								String title;
								String msg;
								int icon;
								
								if ( code == ThreadFinishedListener.OK ) {
									title = Messages.getString("EcfImport.ImportSuccessTitle");
									msg = Messages.getString( "EcfImport.ImportSuccessMessage" );
									icon = SWT.ICON_INFORMATION;
								}
								else {
									title = Messages.getString("EcfImport.ImportErrorTitle");
									msg = Messages.getString( "EcfImport.ImportErrorMessage" );
									icon = SWT.ICON_ERROR;
								}
								
								GlobalUtil.showDialog(shell, title, msg, icon );
							}
						});
					}
				});
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});

		importCatMI.setEnabled( false );
		
		return importCatMI;
	}
	
	/**
	 * Close the current catalogue (if one is opened)
	 * @param menu
	 * @return
	 */
	private MenuItem addCloseCatalogueMI ( Menu menu ) {
		
		final MenuItem closeCatMI = new MenuItem( menu , SWT.NONE );
		closeCatMI.setText( Messages.getString("BrowserMenu.CloseCatalogueCmd") );
		
		closeCatMI.addSelectionListener( new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {

				if ( listener != null )
					listener.buttonPressed( closeCatMI, 
							CLOSE_CAT_MI, null );
				
				if ( mainMenu.getCatalogue() == null )
					return;
				
				mainMenu.getCatalogue().close();
				
				// refresh UI
				mainMenu.refresh();
				
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		return closeCatMI;
	}
	
	
	/**
	 * Add a menu item which allows to open a .catalog file which will be
	 * loaded as database
	 * @param menu
	 */
	private MenuItem addDeleteCatalogueMI ( Menu menu ) {
		
		final MenuItem deleteCatMI = new MenuItem( menu , SWT.NONE );
		deleteCatMI.setText( Messages.getString("BrowserMenu.DeleteCatalogueCmd") );

		deleteCatMI.addSelectionListener( new SelectionAdapter() {
			
			@Override
			public void widgetSelected ( SelectionEvent event ) {
				
				// ask and delete catalogues
				FileActions.deleteCatalogue( shell );
				
				if ( listener != null )
					listener.buttonPressed( deleteCatMI, 
							DELETE_CAT_MI, null );
			}
		} );
		
		return deleteCatMI;
	}

	
	/**
	 * Add a menu item which allows exiting the application
	 * @param menu
	 */
	private MenuItem addExitMI ( Menu menu ) {
		
		final MenuItem exitItem = new MenuItem( menu , SWT.NONE );
		exitItem.setText( Messages.getString("BrowserMenu.ExitAppCmd") );
		
		exitItem.addSelectionListener( new SelectionAdapter() {

			public void widgetSelected ( SelectionEvent e ) {
				
				mainMenu.getShell().close();
				
				if ( listener != null )
					listener.buttonPressed( exitItem, 
							EXIT_MI, null );
			}

		} );
		
		return exitItem;
	}
	
	/**
	 * Refresh all the menu items of the file menu
	 */
	public void refresh () {

		CatalogueDAO catDao = new CatalogueDAO();
		
		// get all the catalogues I have downloaded before and get the size
		boolean hasCatalogues = catDao.getMyCatalogues( Dcf.dcfType ).size() > 0;
			
		// Return if widget disposed
		if ( openMI.isDisposed() )
			return;
		
		// check if there is at least one catalogue available from the 
		// catalogue master table. If not => open disabled
		// can open only if we are not getting updates and we have at least one catalogue downloaded
		openMI.setEnabled( hasCatalogues && !Dcf.isGettingUpdates() && 
				catDao.getMyCatalogues( Dcf.dcfType ).size() > 0 );

		// allow import only if no catalogue is opened
		importCatMI.setEnabled( mainMenu.getCatalogue() == null );
		
		User user = User.getInstance();
		
		// we can download only if we know the user access level and
		// if there are some catalogues which can be downloaded
		// we avoid the possibility to download a catalogue while
		// we are checking the user access level since there may be
		// database conflicts!
		boolean canDownload = user.isUserLevelDefined() && 
				Dcf.getDownloadableCat() != null;

		downloadMI.setEnabled ( canDownload );
		downloadDcMI.setEnabled( canDownload );
		
		// enable close only if there is an open catalogue
		closeMI.setEnabled( mainMenu.getCatalogue() != null );

		DCDAO dcDao = new DCDAO();
		openDcMI.setEnabled(!dcDao.getAll().isEmpty());
		
		// enable delete only if we have at least one catalogue downloaded and we
		// have not an open catalogue (to avoid deleting the open catalogue)
		deleteMI.setEnabled( hasCatalogues && mainMenu.getCatalogue() == null );

		// if we are retrieving the catalogues
		if ( Dcf.isGettingUpdates() ) {
			downloadMI.setText( Messages.getString( "BrowserMenu.DownloadingUpdatesCmd" ) );
			openMI.setText( Messages.getString( "BrowserMenu.DownloadingUpdatesCmd" ) );
			downloadDcMI.setText( Messages.getString( "BrowserMenu.DownloadingUpdatesCmd" ) );
		}
		// if we are getting the user level
		else if ( user.isGettingUserLevel() ) {
			downloadMI.setText( Messages.getString( "BrowserMenu.GettingUserLevelCmd" ) );
			openMI.setText( Messages.getString( "BrowserMenu.OpenCatalogueCmd" ) );
			downloadDcMI.setText( Messages.getString( "BrowserMenu.GettingUserLevelCmd" ) );
		}
		else {
			downloadDcMI.setText( Messages.getString( "BrowserMenu.DownloadDcCmd" ) );
			downloadMI.setText( Messages.getString( "BrowserMenu.DownloadCatalogueCmd" ) );
			openMI.setText( Messages.getString( "BrowserMenu.OpenCatalogueCmd" ) );
		}
	}
}
