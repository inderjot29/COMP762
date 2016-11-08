package atcommentnew;


import java.util.ArrayList;
import java.util.List;


import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BlockComment;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.LineComment;
import org.eclipse.jdt.core.dom.TagElement;

public class CommentVisitor extends ASTVisitor {
	CompilationUnit cu;
	String source;
	List<String> _commentList;
	List<TagElement> _tagsList=new ArrayList<TagElement>();
 
	public CommentVisitor(CompilationUnit cu, String source,List<String> commentList) {
		super();
		this.cu = cu;
		this.source = source;
		this._commentList=commentList;
	}
 
	public boolean visit(LineComment node) {
		int start = node.getStartPosition();
		int end = start + node.getLength();
		String comment = source.substring(start, end);
		_commentList.add(comment);
		//System.out.println(comment);
		return super.visit(node);
	}
 
	public boolean visit(BlockComment node) {
		int start = node.getStartPosition();
		int end = start + node.getLength();
		String comment = source.substring(start, end);
		_commentList.add(comment);
		//System.out.println(comment);
	
		return super.visit(node);
	}
	
	
	
	
	@SuppressWarnings("unchecked")
	public boolean visit(Javadoc node) {
		
		_tagsList.addAll(node.tags());
		
		return super.visit(node);
	}
	
	public List<TagElement> getTags()
	{
		return _tagsList;
	}
	
	
 
	
}
