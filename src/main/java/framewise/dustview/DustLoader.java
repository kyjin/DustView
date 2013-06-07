package framewise.dustview;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Scriptable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Support server-side dust rendering Function. This class load by Rhino JavaScript Engine.
 * 
 * @author chanwook
 * 
 */
public class DustLoader {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	public static final String DEFAULT_COMPILE_SOURCE_NAME = "ServerSideDustCompiler";

	private static final String DEFAULT_ENCODING = "UTF-8";

	public static final String DEFAULT_DUST_JS_FILE_PATH = "/dust/dust-full-1.1.1.js";
	public static final String DEFAULT_DUST_HELPER_JS_FILE_PATH = "/dust/dust-helpers-1.1.0.js";

	public static final String DEFAULT_COMPILE_SCRIPT = "(dust.compile(source, templateKey))";
	public static final String DEFAULT_LOAD_SCRIPT = "(dust.loadSource(compiledSource))";
	public static final String DEFAULT_RENDER_SCRIPT = (
					"{   dust.render( templateKey, JSON.parse(json), "
							+ "function(error, data) { if(error) { writer.write(error);} else { writer.write( data );} } );}"
			);

	
	private String dustJsFilePath = DEFAULT_DUST_JS_FILE_PATH;
	private String dustJsHelperFilePath = DEFAULT_DUST_HELPER_JS_FILE_PATH;
	
	private String encoding = DEFAULT_ENCODING;

	public String compileSourceName = DEFAULT_COMPILE_SOURCE_NAME;
	
	private String compileScript = DEFAULT_COMPILE_SCRIPT;
	private String loadScript = DEFAULT_LOAD_SCRIPT;
	private String renderScript = DEFAULT_RENDER_SCRIPT;

	public static Scriptable globalScope;
	
	public DustLoader() {
	}

	/**
	 * dust context initialize method. must call before running dust
	 */
	public void initializeContext() {
		InputStream dustJsStream = getDustJsStream(getDustJsFilePath());
		InputStream dustHelperJsStream = getDustJsStream(getDustJsHelperFilePath());

		loadDustJsEngine(dustJsStream, dustHelperJsStream);
	}

	/**
	 * Initialize Dust JS Context
	 * 
	 * @param dustJsStream
	 * @param dustHelperJsStream
	 */
	protected void loadDustJsEngine(InputStream dustJsStream, InputStream dustHelperJsStream) {
		try {
			Reader dustJsReader = new InputStreamReader(dustJsStream, encoding);
			Reader dustJsHelperReader = new InputStreamReader(dustHelperJsStream, encoding);

			Context context = Context.enter();
			context.setOptimizationLevel(9);

			globalScope = context.initStandardObjects();
			context.evaluateReader(globalScope, dustJsReader, dustJsFilePath, dustJsStream.available(), null);
			context.evaluateReader(globalScope, dustJsHelperReader, dustJsHelperFilePath,
					dustHelperJsStream.available(), null);
		} catch (Exception e) {
			logger.error("thrown exception when initialize step!", e);
			throw new RuntimeException(e);
		} finally {
			Context.exit();
		}
	}

	/**
	 * Compile HTML Markup that used by Dust.js
	 * 
	 * @param source HTML Markup Source
	 * @param templateKey
	 * @return Compiled HTML Markup(JavaScript Format)
	 */
	public String compile(String source, String templateKey) {
		Context context = Context.enter();

		Scriptable compileScope = context.newObject(globalScope);
		compileScope.setParentScope(globalScope);

		compileScope.put("source", compileScope, source);
		compileScope.put("templateKey", compileScope, templateKey);

		try {
			return (String) context.evaluateString(compileScope, compileScript, compileSourceName, 0, null);
		} catch (JavaScriptException e) {
			throw new RuntimeException("thrown error when compile Dust JS Source", e);
		}
	}

	/**
	 * Load Compiled Markup Source to JavaScript Object
	 *  
	 * @param compiledSource load target HTML Markup
	 */
	public void load(String compiledSource) {
		Context context = Context.enter();

		Scriptable compileScope = context.newObject(globalScope);
		compileScope.setParentScope(globalScope);

		compileScope.put("compiledSource", compileScope, compiledSource);

		try {
			context.evaluateString(compileScope, loadScript, compileSourceName, 0, null);
		} catch (JavaScriptException e) {
			throw new RuntimeException("thrown error when load Dust JS Source", e);
		}
	}

	/**
	 * Rendering Markup. result is binded to Markup with JSON data.
	 * Result is plain text HTML markup, then will write to {@link Writer} object.
	 * 
	 * @param writer
	 * @param templateKey
	 * @param json
	 */
	public void render(Writer writer, String templateKey, String json) {
		Context context = Context.enter();

		Scriptable renderScope = context.newObject(globalScope);
		renderScope.setParentScope(globalScope);

		try {
			renderScope.put("writer", renderScope, writer);
			renderScope.put("json", renderScope, json);
			renderScope.put("templateKey", renderScope, templateKey);

			context.evaluateString(renderScope, renderScript, compileSourceName, 0, null);

		} catch (JavaScriptException e) {
			throw new RuntimeException("thrown error when Rendering Dust JS Source", e);
		}
	}

	/**
	 * Resolve File InputStream by Path
	 * 
	 * @param filePath
	 * @return
	 */
	public InputStream getDustJsStream(String filePath) {
		InputStream resourceStream = getClass().getResourceAsStream(filePath);
		if (resourceStream == null) {
			throw new IllegalArgumentException("uncorrect filePath! '" + filePath + "' does not exist!");
		}
		return resourceStream;
	}

	/*
	 * Getter & Setter Method
	 */
	
	/**
	 * @return
	 */
	public String getDustJsFilePath() {
		return dustJsFilePath;
	}

	/**
	 * @return
	 */
	public String getDustJsHelperFilePath() {
		return dustJsHelperFilePath;
	}
	
	public void setDustJsFilePath(String dustJsFilePath) {
		this.dustJsFilePath = dustJsFilePath;
	}
	
	public void setDustJsHelperFilePath(String dustJsHelperFilePath) {
		this.dustJsHelperFilePath = dustJsHelperFilePath;
	}
	
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}
	
	public void setCompileScript(String compileScript) {
		this.compileScript = compileScript;
	}
	
	public void setLoadScript(String loadScript) {
		this.loadScript = loadScript;
	}
	
	public void setRenderScript(String renderScript) {
		this.renderScript = renderScript;
	}
	
	
}
