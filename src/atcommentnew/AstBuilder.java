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
/**
 * @author iratol
 *
 */
public  class AstBuilder {

	
	//represents renamed element
	public IJavaElement  _renamedElement;
	// stores the string value of compilation unit under processing
	private static String _source;
	// list of comments' text, needed for further processing of comments
	static List<String> _commentList = new ArrayList<String>();
	
	//list containing params of the method under parsing
	private static List<SingleVariableDeclaration> params=null;
	
	private static CompilationUnit _currentCompilationUnit;
	//file to write the content of inferred properties
	private static File file;
	//string to contain the content to be written in the file
	private static String content="";
	
	static Hashtable<String,String> nullTable=new  Hashtable<String,String>();
	static Hashtable<String,String> exceptionsTable=new  Hashtable<String,String>();
	static boolean createdFolder=false;
	// to split the identifiers based on camel casing
	private static final String _regex="(?<=[a-z])((?=[A-Z])|(?=[0-9])|(?=[/_]))|(?<=[A-Z])((?=[A-Z][a-z])|(?=[/_])|(?=[0-9]))|(?<=[/_])((?=[a-z])|(?=[0-9])|(?=[A-Z]))|(?<=[0-9])((?=[a-z])|(?=[/_])|(?=[A-Z]))|([,])";
	
	//counters to keep the count of inferred properties
	private static int _paramNull=0;
	private static int _throwsNull=0;
	private static int knownParamNull=0;
	private static int _throwsNullPerFile=0;
	private static String _projectName;
	private static int _paramNullPerFile=0;
	private static int _unknownNullPerFile=0;
	private static int _nonNullPerFile=0;
	private static int _yesNullPerFile=0;
	
	private AstBuilder(){}
	
	/** 
	 * Getting workspace and deleting all the existing markers if any.*/
	public static void initASTParsing(){
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		// Get all projects in the workspace
		IProject[] projects = root.getProjects();
		for(IProject project :projects)
		{
		try {
			_projectName=project.getName();
			createdFolder=false;
			//if(_projectName.equals("pmd-core"))
			{
			getProjectInfo(project);
			
			System.out.println("Known Param Null in "+_projectName+" "+knownParamNull);
			System.out.println("TotalParams Null in "+_projectName+" "+_paramNull);
			System.out.println("Throws Null in "+_projectName+" "+_throwsNull);
			knownParamNull=0;
			_paramNull=0;
			_throwsNull=0;
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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

	
	/** Parses the ICompilation Unit using AST parser 
	 * @param unit Icompilation unit to part using AST parser
	 * @return Compilation unit after parsed by AST
	 */
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
	 * Reads a ICompilationUnit and creates the AST DOM for parsing 
	 * @param packageName
	 * @throws JavaModelException
	 */
	private static void createAST(ICompilationUnit unit,String packageName) throws JavaModelException {
		
		
		
		// now create the AST for the ICompilationUnits
		CompilationUnit compilationUnit = parse(unit);
		//used to get line and block comments text
		_currentCompilationUnit=compilationUnit;
		
		_source=unit.getSource();
		_commentList=new ArrayList<String>();
		//writing import statements in the file
		writeImportsIntoContentString(compilationUnit);
		//writing package declaration in the file
		content="package "+packageName+";\n\n";
		
		//start with the top level declaration and traverse the AST tree to get type declarations and methods
		parseTypes();
		
		//check if the file contains yes nulls or non nulls as unknown nulls are not of much use. Only needed to get accuracy set 
		//if((_yesNullPerFile+_nonNullPerFile)>0)
		{
				createInferFileForUnit(unit);
				//write the file  content to file 	
				writeToFile();
		}
	}
	
	
	/**
	 * Creates a file with extension .infer for every class file parsed
	 * @param unit
	 */
	private static void createInferFileForUnit(ICompilationUnit unit)
	{
		IPath withoutExtension=unit.getPath().removeFileExtension();
		
		boolean success=false;

		//String path="/home/2015/iratol/git/atComment/programs/jetuml/NullProperties";
		//String path="/home/2015/iratol/git/atComment/programs/"+_projectName+"/NullProperties";
		
		String path="C:\\cygwin64\\home\\inder\\programs\\pmd\\NullProperties";

		if(!createdFolder)
		{
			
			success=new File(path).mkdir();
			createdFolder=true;
		}
		createFile(path+"/"+withoutExtension.lastSegment()+".infer");
		
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
	
	private static void writeImportsIntoContentString(CompilationUnit unit)
	{
		//get import statements
				List<ImportDeclaration> imports=unit.imports();
				if(imports!=null && imports.size()>0)
				{
					for(ImportDeclaration imp : imports)
					{
						content+="import "+imp.getName()+";\n";
					}
				}
				content+="\n";
	}
	
	private static void writeToFile()
	{
		FileWriter fw;
		try {
			fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			knownParamNull+=nullTable.size();
			
			String nullValues="/** Number of Unknown nulls in the file :"+_unknownNullPerFile+"\n * Number of Yes nulls in the file :"+_yesNullPerFile+"\n * Number of Non nulls in the file :"+_nonNullPerFile+"\n * number of throws null in file :"+_throwsNullPerFile+"\n */\n\n";
			content=nullValues+content;
			bw.write(content);
			_throwsNullPerFile=0;
			_unknownNullPerFile=0;
			_nonNullPerFile=0;
			_yesNullPerFile=0;
			bw.close();
			//System.out.println("\n Param tags:"+paramsTags);
			//System.out.println("Throws tags:"+throwsTag);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		
	}
	
	private  static void parseTypes()
	{

		ClassVisitor visitor = new ClassVisitor();
		_currentCompilationUnit.accept(visitor);
		String mod="";
		
		//loop through every type present in the source code and look for null properties
		for (TypeDeclaration classes: visitor.getClasses()) {
			String className=classes.getName().getIdentifier();
			// getting modifiers for the current class to write the specific signature in the file
			if(classes.modifiers()!=null && classes.modifiers().size()>0)
			{
				List<IExtendedModifier> modifiers =classes.modifiers();
				for(IExtendedModifier modifier : modifiers)
				{
					if(modifier.isModifier())
						mod+=modifier.toString()+" ";
				}
			}
			
			//check if the type is an interface or class, and accordingly use the appropriate word- extends/implements
			if(!classes.isInterface())
				content+=mod+" class "+className;
			else
				content+=mod+" interface "+className;
			
			//getting the name of super class or super interface	
			writeSuperClassInfo(classes);
			
			// traverse methods present in the type 
			parseMethods(classes.getMethods());
			
			
			content+="}\n\n";
		}
			
	}
	
	private static void writeSuperClassInfo(TypeDeclaration classes)
	{
		Type superClass=classes.getSuperclassType();
		if(superClass!=null && !superClass.toString().isEmpty())
			content+=" extends "+superClass.toString();
		List<Type> superInterface=classes.superInterfaceTypes();
		if(superInterface!=null && superInterface.size()>0 && !superInterface.get(0).toString().isEmpty())
			content+=" implements "+superInterface.get(0).toString();
		
		content+="{\n\n";
	}
	
	//parses methods present in a type declaration AST node
	private static void parseMethods(MethodDeclaration[] methodDeclarations)
	{
		for(MethodDeclaration method:methodDeclarations)
		{
			
			if(method.getJavadoc()!=null)
			{
				String methodMod="";
				CommentVisitor javadocVisitor=new CommentVisitor(_currentCompilationUnit,_source,_commentList);
				method.getJavadoc().accept(javadocVisitor);
				ParseTags(javadocVisitor.getTags(),method);
				String name=method.getName().getIdentifier();
				if(method.modifiers()!=null && method.modifiers().size()>0)
				{
					List<IExtendedModifier> modifiers =method.modifiers();
					for(IExtendedModifier modifier : modifiers)
					{
						if(modifier.isModifier())
							methodMod+=modifier.toString()+" ";
					}
				}
				
				
				
				String returnType="";
				if(method.getReturnType2()!=null)
					returnType=method.getReturnType2().toString();
				content+=methodMod+" "+returnType+" "+name;
				//getting formal parameters and parsing the parameter nodes
				List<SingleVariableDeclaration> params=method.parameters();
				parseParameters(params);
				
			}
			
			
		}
	}
	
	/**
	 * Parses the parameters of a method to write the complete signature of a method in file
	 * @param params list of formal parameters
	 */
	private static void parseParameters(List<SingleVariableDeclaration> params)
	{
		content+="(";
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
		content+=")\n\n";
	}
	 
	
	
	
	/**
	 * @param tags javadoc tags
	 * @param method method related to the javadoc tags
	 */
	@SuppressWarnings("unchecked")
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
		
		annotateMethodsWithNullProperties();
		
		
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
			if(!parameterName.equals("") && !processParamTag(text).equals(""))
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
		StringTokenizer multiTokenizer = new StringTokenizer(text, ",://.-<>(){}[] ");
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
				case "non":
					if(!annotation.equals(""))
						return "@NonNull";
					else
						negated=true;
					break;
				}
			}
			
		}
		if(annotation.equals(""))
		{
		
				return "@UnknownNull";
				
		}
		
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
				{
					yesNulls++;
				}
				else if(checkNonNull(token.toLowerCase(),text))
				{
					nonNulls++;
				}
			}
			
			if(tokens.size()==1)
			{
				 if(yesNulls==tokens.size()) {
					 exceptionsTable.put(entry, exceptionName);
						nullTable.replace(entry,"@YesNull");
						return true;
				 }
				 else if(nonNulls==tokens.size()){
					 exceptionsTable.put(entry, exceptionName);
						nullTable.replace(entry,"@NonNull");
						return true;
				 }
			}
			else if(tokens.size()>1)
			{
				if(yesNulls>=tokens.size()-1){
					 exceptionsTable.put(entry, exceptionName);
						nullTable.replace(entry,"@YesNull");
						return true;
				 }
				 else if(nonNulls>=tokens.size()-1){
					 exceptionsTable.put(entry, exceptionName);
						nullTable.replace(entry,"@NonNull");
						return true;
				 }
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
				||text.contains("never")||text.contains("non")));
	}
	
	private static boolean checkYesNull(String param,String text)
	{
		return (text.contains(param) && 
				text.contains("null") 
				&& !text.contains("not")
				&&!text.contains("never")&&!text.contains("non"));
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


	private static void annotateMethodsWithNullProperties()
	{
		writeOtherExceptionAnnotation();
		
		if(nullTable!=null && nullTable.size()>0)
		{
			for(Map.Entry<String,String> entry:nullTable.entrySet() )
			{
				_paramNull++;
				_paramNullPerFile++;
				// if else used to count the final number of different tags 
				if(entry.getValue().equals("@YesNull"))
				{
					_yesNullPerFile++;
					writeYesNullParam(entry);
				}
				else if(entry.getValue().equals("@NonNull"))
				{
					_nonNullPerFile++;
					writeNonNullParam(entry);
				}
				else
				{
					_unknownNullPerFile++;
					writeNonNullParam(entry);
				}
			}
			
		}
		else if(params!=null && params.size()>0)
		{
			//if not in nullTable, means param tag was not present in the comment, tag all params unknownNuLL in that case
			tagNonJavadocParams();
			
		}
		
	}
	
	private static void writeOtherExceptionAnnotation()
	{
		if(exceptionsTable!=null && exceptionsTable.size()>0)
		{
			for(Map.Entry<String,String> entry:exceptionsTable.entrySet() )
			{
				if(entry.getKey().equals("@OtherException"))
				{
					content+=entry.getKey()+"("+entry.getValue()+")\n";
					
				}
			}
		}
		
	}
	
	private static void writeYesNullParam(Map.Entry<String,String> entry)
	{
		if(exceptionsTable.get(entry.getKey())!=null)
		{
		content+=entry.getValue()+"("+entry.getKey()+"==null =>"+exceptionsTable.get(entry.getKey())+")\n";
		_throwsNullPerFile++;
		_throwsNull++;
		}
		else
		{
			content+=entry.getValue()+"("+entry.getKey()+"=="+"null)\n";
			
		}
	}
	
	private static void writeNonNullParam(Map.Entry<String,String> entry)
	{
		if(exceptionsTable.get(entry.getKey())!=null)
		{
			content+="@YesNull("+entry.getKey()+"==null =>"+exceptionsTable.get(entry.getKey())+")\n";
			_throwsNullPerFile++;
			_throwsNull++;
		}
		else
			content+=entry.getValue()+"("+entry.getKey()+")\n";
	}
	
	private static void tagNonJavadocParams()
	{
		_paramNull++;
		_unknownNullPerFile++;
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







