package atcommentnew;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.MethodRef;
import org.eclipse.jdt.core.dom.MethodRefParameter;

public class MethodVisitor extends ASTVisitor {
	List<MethodDeclaration> methodDeclarations = new ArrayList<MethodDeclaration>();
	List<MethodInvocation> methodInvocations = new ArrayList<MethodInvocation>();
	List<MethodRef> methodRefs = new ArrayList<MethodRef>();
	List<MethodRefParameter> methodRefParameters = new ArrayList<MethodRefParameter>();
	List<ConstructorInvocation> constructors = new ArrayList<ConstructorInvocation>();


	@Override
	public boolean visit(MethodDeclaration node) {
		
		methodDeclarations.add(node);
		return super.visit(node);
	}

	public List<MethodDeclaration> getMethodDeclarations() {
		return methodDeclarations;
	}
	
	@Override
	public boolean visit(MethodInvocation node) {
		
		methodInvocations.add(node);
		return super.visit(node);
	}

	public List<MethodInvocation> getMethodInvocations() {
		return methodInvocations;
	}
	
	@Override
	public boolean visit(MethodRef node) {
		methodRefs.add(node);
		return super.visit(node);
	}

	public List<MethodRef> getMethodRefs() {
		return methodRefs;
	}

	@Override
	public boolean visit(MethodRefParameter node) {
		methodRefParameters.add(node);
		return super.visit(node);
	}

	public List<MethodRefParameter> getMethodRefParameters() {
		return methodRefParameters;
	}
	
	@Override
	public boolean visit(ConstructorInvocation node) {
		constructors.add(node);
		return super.visit(node);
	}

	public List<ConstructorInvocation> getConstructors() {
		return constructors;
	}


}
