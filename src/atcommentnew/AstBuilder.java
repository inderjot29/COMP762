package atcommentnew;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.*;



/**
 * @author inder
 *
 */
public  class AstBuilder {

	
	//represents renamed element
	public IJavaElement  _renamedElement;
	// stores the string value of compilation unit under processing
	private static String _source;
	// list of comments' text, needed for further processing of comments
	static List<String> _commentList = new ArrayList<String>();
	// instance of identifier used to capture all the required properties of a renamed java element.
	
	private static CompilationUnit _currentCompilationUnit;
	private static ICompilationUnit _cunit;
	private String _currentPackage;
	private static String _projectName;
	private int numberOfIdentifiers=0;
	private static File file;
	private static String content="";
	static Hashtable<String,String> nullTable=new  Hashtable<String,String>();
	static Hashtable<String,String> exceptionsTable=new  Hashtable<String,String>();
	static boolean createdFolder=false;
	private static final String _regex="(?<=[a-z])((?=[A-Z])|(?=[0-9])|(?=[/_]))|(?<=[A-Z])((?=[A-Z][a-z])|(?=[/_])|(?=[0-9]))|(?<=[/_])((?=[a-z])|(?=[0-9])|(?=[A-Z]))|(?<=[0-9])((?=[a-z])|(?=[/_])|(?=[A-Z]))|([,])";
	
	
	private AstBuilder()
	{
		
		
	}
	
	
	
	

	/** 
	 * Getting workspace and deleting all the existing markers if any.*/
	public static void initASTParsing(){
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		
		/*Runnable runnable = new Runnable() {
			public void run() {
				try {
					IPath projectDotProjectFile = new Path("/"+_projectName + "/.project");
					IProjectDescription projectDescription = workspace.loadProjectDescription(projectDotProjectFile);
					IProject project = workspace.getRoot().getProject(projectDescription.getName());
					JavaCapabilityConfigurationPage.createProject(project, projectDescription.getLocationURI(),	null);
					//project.create(null);
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		};
		 
		// and now get the workbench to do the work
		//final IWorkbench workbench = PlatformUI.getWorkbench();
		//workbench.getDisplay().syncExec(runnable);*/
		 

		// Get all projects in the workspace
		IProject project = root.getProject("commons-collections");
		try {
			getProjectInfo(project);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	private static void getProjectInfo(IProject project) throws JavaModelException, CoreException  {
		//System.out.println("Working in project " + project.getName());
		// check if we have a Java project
		
		if (project.isNatureEnabled("org.eclipse.jdt.core.javanature")) {
			IJavaProject javaProject = JavaCore.create(project);
			getPackagesInfo(javaProject);
		}

	}

	private static void getPackagesInfo(IJavaProject javaProject)
			throws JavaModelException {
		IPackageFragment[] packages = javaProject.getPackageFragments();
		for (IPackageFragment mypackage : packages) {
			// Package fragments include all packages in the
			// classpath
			// We will only look at the package from the source
			// folder
			// K_BINARY would include also included JARS, e.g.
			// rt.jar
			if(mypackage.getPath().toString().toLowerCase().contains("test") ||
					mypackage.getPath().toString().toLowerCase().contains("resources"))
				continue;
			if (mypackage.getKind() == IPackageFragmentRoot.K_SOURCE) {
				//System.out.println("Parsing Package " + mypackage.getElementName());
				getCompilationUnitList(mypackage);
				

			}

		}
	}

	/*
	 * Checking if the package is non-test package, and if the files traversed are non-test files or not. */
	private static void getCompilationUnitList(IPackageFragment mypackage) throws JavaModelException{
		for (ICompilationUnit unit : mypackage.getCompilationUnits()) {
			
			//create AST for the compilation Unit.
			createAST(unit,mypackage.getElementName());
		}
	}

	private static CompilationUnit parse(ICompilationUnit unit) {
		ASTParser parser;
		try {
			parser = ASTParser.newParser(AST.JLS8);
			parser.setKind(ASTParser.K_COMPILATION_UNIT);
			parser.setSource(unit);
			parser.setResolveBindings(true);
			final CompilationUnit cu=(CompilationUnit) parser.createAST(null);
			
			return cu;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
		
	}


	

	/**
	 * Reads a ICompilationUnit and creates the AST DOM for manipulating the
	 * @param packageName
	 * @throws JavaModelException
	 */
	private static void createAST(ICompilationUnit unit,String packageName) throws JavaModelException {
		
		IPath withoutExtension=unit.getPath().removeFileExtension();
		
		boolean success=false;
		String path="D://Eclipse Projects/runtime-EclipseApplication/collections/NullProperties";
		if(!createdFolder)
		{
			
			success=new File(path).mkdir();
			createdFolder=true;
		}
		
			createFile(path+"/"+withoutExtension.lastSegment()+".infer");
		
		
		
		// now create the AST for the ICompilationUnits
		CompilationUnit compilationUnit = parse(unit);
		//used to get line and block comments text
		_currentCompilationUnit=compilationUnit;
		
		_source=unit.getSource();
		_commentList=new ArrayList<String>();
		String toBeWritten=compilationUnit.getJavaElement().getElementName();
		content="package "+packageName+";\n\n";
		
		//parse comments present in every package
		parseTypes();
		
		//only parsing the package which contains renamed identifier. Need parent class name ,modifiers etc. to determine scope.
		

	
			
		//parseMethods(null);
				
			
		writeToFile();	
	}
	
	private static void createFile(String path)
	{
		 file = new File(path);

		// if file doesnt exists, then create it
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		
	}
	
	private static void writeToFile()
	{
		FileWriter fw;
		try {
			fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(content);
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		
	}
	
	private  static void parseTypes()
	{

		ClassVisitor visitor = new ClassVisitor();
		_currentCompilationUnit.accept(visitor);
		
		for (TypeDeclaration classes: visitor.getClasses()) {
			String className=classes.getName().getIdentifier();
			String mod=mapModifier(classes.getModifiers());
			if(classes.isInterface())
				content+=mod+" interface "+className+"{\n\n";
			else
				content+=mod+" class "+className+"{\n\n";
			for(MethodDeclaration method:classes.getMethods())
			{
				
				if(method.getJavadoc()!=null)
				{
					CommentVisitor javadocVisitor=new CommentVisitor(_currentCompilationUnit,_source,_commentList);
					method.getJavadoc().accept(javadocVisitor);
					ParseTags(javadocVisitor.getTags(),method);
					String name=method.getName().getIdentifier();
					
					String modifier=mapModifier(method.getModifiers());
					String returnType="";
					if(method.getReturnType2()!=null)
						returnType=method.getReturnType2().toString();
					List<SingleVariableDeclaration> params=method.parameters();
					content+=modifier+" "+returnType+" "+name+"(";
					if(params!=null && params.size()>0)
					{
						int count=0;
						for(SingleVariableDeclaration param:params)
						{
							if(count>0)
								content+=", ";
							content+=param.getType()+" ";
							content+=param.getName().toString();
							count++;
							
						}
					}
					content+=");\n\n";
				}
				
				
			}
			content+="}\n\n";
		}
			
	}
	
	private static String mapModifier(int mod)
	{
		if(Modifier.isDefault(mod))
			return "";
		else if(Modifier.isPrivate(mod))
			return "private";
		else if(Modifier.isProtected(mod))
			return "protected";
		else if(Modifier.isPublic(mod))
			return "public";
		return null;
	}
	
	private static List<SingleVariableDeclaration> params=null;
	private static void ParseTags(List<TagElement> tags,MethodDeclaration method)
	{
		nullTable=new  Hashtable<String,String>();
		exceptionsTable=new  Hashtable<String,String>();
		params=method.parameters();
		
		for(TagElement tag: tags)
		{
			if(tag.getTagName()!=null)
			{
					if(tag.getTagName().equalsIgnoreCase("@param"))
					{
						splitTagandProcess(tag,TagElement.TAG_PARAM);
					}
					else if(tag.getTagName().equalsIgnoreCase("@throws"))
					{
						splitTagandProcess(tag,TagElement.TAG_THROWS);
					}
			}
		}
		if(exceptionsTable!=null && exceptionsTable.size()>0)
		{
			for(Map.Entry<String,String> entry:exceptionsTable.entrySet() )
			{
			if(entry.getKey().equals("@OtherException"))
				content+=entry.getKey()+"("+entry.getValue()+")\n";
			}
		}
		
		if(nullTable!=null && nullTable.size()>0)
		{
			for(Map.Entry<String,String> entry:nullTable.entrySet() )
			{
				if(entry.getValue().equals("@YesNull"))
					content+=entry.getValue()+"("+entry.getKey()+"=="+"null =>"+exceptionsTable.get(entry.getKey())+")\n";
				else
					content+=entry.getValue()+"("+entry.getKey()+")\n";
			}
			
		}
		else if(params!=null && params.size()>0)
		{
			
			content+="@UnknownNull(";
			int count=0;
			for(SingleVariableDeclaration param:params)
			{
				if(count>0)
					content+=", ";
				content+=param.getName().toString();
				count++;
				
			}
			content+=")\n";
		}
		
		
	}
	
	
	private static void splitTagandProcess(TagElement tag,String tagType)
	{
		String parameterName="";
		String exceptionName="";
		String text="";
		
		if(tagType==TagElement.TAG_PARAM)
		{
			if(tag.fragments()!=null && tag.fragments().size()>0)
			{
				if(tag.fragments().get(0) instanceof SimpleName)
					parameterName=((SimpleName)tag.fragments().get(0)).toString();
				if(tag.fragments().size()>1 && tag.fragments().get(1) instanceof TextElement)
					text=((TextElement)tag.fragments().get(1)).toString().toLowerCase();
			}
			if(!parameterName.equals(""))
				nullTable.put(parameterName ,processParamTag(text));
		}
		else if( tagType==TagElement.TAG_THROWS)
		{
			if(tag.fragments()!=null && tag.fragments().size()>0)
			{
				if(tag.fragments().get(0) instanceof SimpleName)
					exceptionName=((SimpleName)tag.fragments().get(0)).toString();
				if(tag.fragments().size()>1 && tag.fragments().get(1) instanceof TextElement)
					text=((TextElement)tag.fragments().get(1)).toString().toLowerCase();
			}
			processThrowsTag(text,exceptionName);
				
			
		}
		
		
	}
	
	
	private static String processParamTag(String text)
	{
		String annotation="";
		StringTokenizer multiTokenizer = new StringTokenizer(text, ",://.- ");
		boolean negated=false;
		while (multiTokenizer.hasMoreTokens())
		{
			String nextToken=multiTokenizer.nextToken();
			nextToken=nextToken.toLowerCase();
			
			if(!nextToken.matches("([(\\)]+|[(*)]+)"))
			{
				switch(nextToken)
				{
				case "null":
					if(negated)
						annotation="@NonNull";
					else
						annotation= "@YesNull";
					break;
				case "not":
					if(!annotation.equals(""))
						return "@NonNull";
					else
						negated=true;
					break;
				case "never":
					if(!annotation.equals(""))
						return "@NonNull";
					else
						negated=true;
					break;
				}
			}
			
		}
		if(annotation.equals(""))
				return "@UnknownNull";
		
		return annotation;
		
	}
	
	
	private static void processThrowsTag(String text,String exceptionName)
	{
		
		if(nullTable.size()>0)
		{
			for(Map.Entry<String,String> entry:nullTable.entrySet() )
			{
				if(!exceptionName.equals(""))
				{
					
					 if(checkYesNull(entry.getKey().toLowerCase(),text))
					{
						exceptionsTable.put(entry.getKey(), exceptionName);
						nullTable.replace(entry.getKey(),"@YesNull");
						
					}
					else if(checkNonNull(entry.getKey().toLowerCase(),text))
					{
						exceptionsTable.put(entry.getKey(), exceptionName);
						nullTable.replace(entry.getKey(),"@NonNull");
						
					}
					else if(checkYesNull("either",text)|| checkYesNull("both",text))
					{
						exceptionsTable.put(entry.getKey(), exceptionName);
						nullTable.replace(entry.getKey(),"@YesNull");
					}
					else if (checkNonNull("either",text)|| checkNonNull("both",text))
					{
						exceptionsTable.put(entry.getKey(), exceptionName);
						nullTable.replace(entry.getKey(),"@NonNull");
					}
					 else if(!checkTokensAndType(entry.getKey(),text,exceptionName))
					{
						exceptionsTable.put("@OtherException", exceptionName);
					
					}
				}
			}
			
		}
		
		
		
		
	}
	
	private static boolean checkTokensAndType(String entry,String text,String exceptionName)
	{
		List<String> tokens=checkOneTokenParam(entry);
		if(!checkType(entry,text,exceptionName)&& tokens.size()>0)
		//if(tokens.size()>0)
		{
			
			int yesNulls=0;
			int nonNulls=0;
			for(String token:tokens)
			{
				if(checkYesNull(token.toLowerCase(),text))
					yesNulls++;
				else if(checkYesNull(token.toLowerCase(),text))
					nonNulls++;
			}
			 if(yesNulls==tokens.size())
			 {
				 exceptionsTable.put(entry, exceptionName);
					nullTable.replace(entry,"@YesNull");
					return true;
			 }
			 else if(nonNulls==tokens.size())
			 {
				 exceptionsTable.put(entry, exceptionName);
					nullTable.replace(entry,"@NonNull");
					return true;
			 }
		}
		return false;
	}
	
	private static boolean checkType(String entry,String text,String exceptionName)
	{
		 if(params!=null && params.size()>0)
			{
				for(SingleVariableDeclaration param:params)
				{
					if(param.getName().getIdentifier().equalsIgnoreCase(entry))
					{
						 if(checkYesNull(param.getType().toString().toLowerCase(),text))
							{
								exceptionsTable.put(entry, exceptionName);
								nullTable.replace(entry,"@YesNull");
								return true;
								
							}
							else if(checkNonNull(param.getType().toString().toLowerCase(),text))
							{
								exceptionsTable.put(entry, exceptionName);
								nullTable.replace(entry,"@NonNull");
								return true;
								
							}
					}
				}
			}
		 return false;
	}
	
	private static boolean checkNonNull(String param,String text)
	{
		return (text.contains(param) && 
				text.contains("null") 
				&& (text.contains("not")
				||text.contains("never")));
	}
	
	private static boolean checkYesNull(String param,String text)
	{
		return (text.contains(param) && 
				text.contains("null") 
				&& !text.contains("not")
				&&!text.contains("never"));
	}
	
	
	private static List<String> checkOneTokenParam(String param)
	{
		List<String> tokens=new ArrayList<String>();
		for(String itoken:(param).split(_regex))
		{
			if(!tokens.contains(itoken)&& itoken.matches("([A-Z]+)|([a-z]+)|([A-Z]+[a-z]+)"))
				tokens.add(itoken.toLowerCase());
	
		}
		return tokens;
	}

}






