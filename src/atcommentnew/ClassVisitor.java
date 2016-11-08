package atcommentnew;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.*;

public class ClassVisitor extends ASTVisitor {
	List<ClassInstanceCreation> classInstances = new ArrayList<ClassInstanceCreation>();
	List<AnonymousClassDeclaration> anonymousClasses = new ArrayList<AnonymousClassDeclaration>();
	List<TypeDeclaration> classdeclaration = new ArrayList<TypeDeclaration>();
	List<EnumDeclaration> enumdeclaration = new ArrayList<EnumDeclaration>();
	
	@Override
	public boolean visit(ClassInstanceCreation node) {
		
		classInstances.add(node);
		return super.visit(node);
	}

	public List<ClassInstanceCreation> getClassInstances() {
		return classInstances;
	}

	@Override
	public boolean visit(AnonymousClassDeclaration node) {
		anonymousClasses.add(node);
		return super.visit(node);
	}

	public List<AnonymousClassDeclaration> getAnonymousClasses() {
		return anonymousClasses;
	}
	
	@Override
	public boolean visit(TypeDeclaration node) {
		classdeclaration.add(node);
		return super.visit(node);
	}

	public List<TypeDeclaration> getClasses() {
		return classdeclaration;
	}
	
	@Override
	public boolean visit(EnumDeclaration node) {
		enumdeclaration.add(node);
		return super.visit(node);
	}

	public List<EnumDeclaration> getEnums() {
		return enumdeclaration;
	}
}
