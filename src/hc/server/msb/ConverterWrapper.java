package hc.server.msb;

public class ConverterWrapper extends Processor {
	Converter fp;
	
	public ConverterWrapper(final Converter fp){
		super(fp.getName(), Workbench.TYPE_CONVERTER_PROC, fp.__context);
		this.fp = fp;
	}
	
	@Override
	protected final void __startup() {
		fp.startup();//in user thread group
	}

	@Override
	protected final void __shutdown() {
		fp.shutdown();//in user thread group
	}

	@Override
	public final void response(final Message msg){//in user thread group
		final Message target = Workbench.messagePool.getFreeMessage();
		
		if(msg.ctrl_is_downward == false){
			try{
				fp.upConvert(msg, target);
				msg.cloneHeaderTo(target);
				target.ctrl_dev_id = preMsg.ctrl_dev_id;
				workbench.V = workbench.O ? false : workbench.log("{" + project_id + "/" + name + "} upConvert from :" + msg.toString() + "\n\tto free message :" + target.toString());
				fp.__forward(target);
			}catch (final Throwable e) {
				workbench.err("fail upProcess on Converter [" + name + "] at " + msg.toString());
				e.printStackTrace();
				Workbench.messagePool.recycle(target, workbench);
			}
		}else{
			try{
				fp.downConvert(msg, target);
				msg.cloneHeaderTo(target);
				target.ctrl_dev_id = preMsg.ctrl_dev_id;
				workbench.V = workbench.O ? false : workbench.log("{" + project_id + "/" + name + "} downConvert from :" + msg.toString() + "\n\tto free message :" + target.toString());
				fp.__forward(target);
			}catch (final Throwable e) {
				workbench.err("fail downProcess on Converter [" + name + "] at " + msg.toString());
				e.printStackTrace();
				Workbench.messagePool.recycle(target, workbench);
			}
		}
	}
	
	@Override
	public final String getIoTDesc(){
		return this.classSimpleName + super.getIoTDesc();
	}
	
//	@Override
//	public final String toString() {//please use getIoTDesc
//		return this.getClass().getSimpleName() + super.getProcDesc();
//	}
}
