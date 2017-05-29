package third.quartz.spi;

/**
 * Allows different strategies for scheduling threads. The {@link #initialize()}
 * method is required to be called before the first call to
 * {@link #execute(Thread)}. The Thread containing the work to be performed is
 * passed to execute and the work is scheduled by the underlying implementation.
 *
 * @author matt.accola
 * @version $Revision: 1.1 $ $Date: 2017/05/21 04:24:27 $
 */
public interface ThreadExecutor {

    /**
     * Submit a task for execution
     *
     * @param thread the thread to execute
     */
    void execute(Thread thread);

    /**
     * Initialize any state prior to calling {@link #execute(Thread)}
     */
    void initialize();
}
