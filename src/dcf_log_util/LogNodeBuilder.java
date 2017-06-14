package dcf_log_util;

import java.util.ArrayList;
import java.util.Collection;

import dcf_webservice.DcfResponse;

/**
 * Builder to create a single LogNode step by step.
 * @author avonva
 *
 */
public class LogNodeBuilder {

	private String name;
	private DcfResponse result;
	private Collection<String> opLogs;

	public LogNodeBuilder() {
		opLogs = new ArrayList<>();
	}

	public LogNodeBuilder setName(String name) {
		this.name = name;
		return this;
	}
	public LogNodeBuilder setResult(String result) {
		this.result = DcfResponse.valueOf( result );
		return this;
	}
	public LogNodeBuilder addOpLog( String opLog ) {
		opLogs.add( opLog );
		return this;
	}

	public LogNode build() {
		return new LogNode( name, result, opLogs );
	}
}
