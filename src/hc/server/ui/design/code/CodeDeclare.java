package hc.server.ui.design.code;

public class CodeDeclare {
	public int pre_var_tag_ins_or_global = CodeHelper.VAR_UN_INIT;
	public boolean preCodeSplitIsDot;
	public boolean varIsNotValid;
	public int preCodeType;
	
	public static CodeDeclare buildNewInstance() {
		return new CodeDeclare();
	}
}
