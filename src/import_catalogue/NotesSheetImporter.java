package import_catalogue;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;

import catalogue.Catalogue;
import catalogue.ReleaseNotesOperation;
import catalogue_browser_dao.ReleaseNotesOperationDAO;
import naming_convention.Headers;
import open_xml_reader.ResultDataSet;
import sheet_converter.NotesSheetConverter;

public class NotesSheetImporter extends SheetImporter<ReleaseNotesOperation> {

	private Catalogue catalogue;
	
	public NotesSheetImporter( Catalogue catalogue, ResultDataSet data) {
		super(data);
		this.catalogue = catalogue;
	}

	@Override
	public ReleaseNotesOperation getByResultSet(ResultDataSet rs) {

		// get all the ops related to the current excel row
		// separating the operation info (they are $ separated)
		ReleaseNotesOperation op = null;
		try {
			op = getByExcelResultSet( rs );
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return op;
	}
	
	
	/**
	 * Get the operation starting from an excel result set
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	public static ReleaseNotesOperation getByExcelResultSet(ResultSet rs) throws SQLException {

		String name = rs.getString( Headers.OP_NAME );
		Timestamp date = rs.getTimestamp( Headers.OP_DATE );
		String info = rs.getString( Headers.OP_INFO );
		int groupId = rs.getInt( Headers.OP_GROUP );

		// create a release note operation with a temp group id
		ReleaseNotesOperation op = new ReleaseNotesOperation(name, 
				date, info, groupId);

		return op;
	}

	@Override
	public Collection<ReleaseNotesOperation> getAllByResultSet(ResultDataSet rs) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void insert( Collection<ReleaseNotesOperation> ops ) {
		
		ReleaseNotesOperationDAO opDao = 
				new ReleaseNotesOperationDAO( catalogue );
		
		opDao.insert( ops );
	}

	@Override
	public void end() {
		// TODO Auto-generated method stub
		
	}
}
