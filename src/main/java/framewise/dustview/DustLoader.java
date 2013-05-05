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
 * @author chanwook
 * 
 */
public class DustLoader {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	public static final String DEFAULT_DUST_JS_FILE_PATH = "/dust/dust-full-1.1.1.js";
	public static final String DEFAULT_DUST_HELPER_JS_FILE_PATH = "/dust/dust-helpers-1.1.0.js";

	public static final String DEFAULT_COMPILE_SCRIPT = "(dust.compile(source, templateKey))";
	public static final String DEFAULT_LOAD_SCRIPT = "(dust.loadSource(compiledSource))";
	public static final String DEFAULT_RENDER_SCRIPT = ("{   dust.render( templateKey, JSON.parse(json), "
			+ "function(error, data) { if(error) { writer.write(error);} else { writer.write( data );} } );}");

	private String dustJsFilePath = DEFAULT_DUST_JS_FILE_PATH;
	private String dustJsHelperFilePath = DEFAULT_DUST_HELPER_JS_FILE_PATH;

	private String compileScript = DEFAULT_COMPILE_SCRIPT;
	private String loadScript = DEFAULT_LOAD_SCRIPT;
	private String renderScript = DEFAULT_RENDER_SCRIPT;

	public static Scriptable globalScope;

	public DustLoader() {
	}

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
			Reader dustJsReader = new InputStreamReader(dustJsStream, "UTF-8");
			Reader dustJsHelperReader = new InputStreamReader(dustHelperJsStream, "UTF-8");

			Context context = Context.enter();
			context.setOptimizationLevel(9);

			globalScope = context.initStandardObjects();
			context.evaluateReader(globalScope, dustJsReader, "dust-full-1.1.1.js", dustJsStream.available(), null);
			context.evaluateReader(globalScope, dustJsHelperReader, "dust-helpers-1.1.0.js",
					dustHelperJsStream.available(), null);
		} catch (Exception e) {
			logger.error("thrown exception when initialize step!", e);
			throw new RuntimeException(e);
		} finally {
			Context.exit();
		}
	}

	public String compile(String source, String templateKey) {
		Context context = Context.enter();

		Scriptable compileScope = context.newObject(globalScope);
		compileScope.setParentScope(globalScope);

		compileScope.put("source", compileScope, source);
		compileScope.put("templateKey", compileScope, templateKey);

		try {
			return (String) context.evaluateString(compileScope, compileScript, "JDustCompiler", 0, null);
		} catch (JavaScriptException e) {
			throw new RuntimeException("thrown error when compile Dust JS Source", e);
		}
	}

	/**
	 * @param compiledSource
	 */
	public void load(String compiledSource) {
		Context context = Context.enter();

		Scriptable compileScope = context.newObject(globalScope);
		compileScope.setParentScope(globalScope);

		compileScope.put("compiledSource", compileScope, compiledSource);

		try {
			context.evaluateString(compileScope, loadScript, "JDustCompiler", 0, null);
		} catch (JavaScriptException e) {
			throw new RuntimeException("thrown error when load Dust JS Source", e);
		}
	}

	/**
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

			context.evaluateString(renderScope, renderScript, "JDustCompiler", 0, null);

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
}
