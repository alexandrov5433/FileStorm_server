package server.filestorm.thread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ThreadExecutorService {
    private final ExecutorService executor;

    @Autowired
    private PlatformTransactionManager transactionManager;

    public ThreadExecutorService() {
        this.executor = Executors.newCachedThreadPool();
        System.out.println("\n#####\n" + this.executor.hashCode() + "\n#####\n");
    }

    @SuppressWarnings("unused")
    public void execute(Runnable runnable) {
        executor.execute(() -> {
            TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.executeWithoutResult(status -> {
                runnable.run();
            });
        });
    }

    // Spring will initiate a roll-back automatically if an unhandled
    // RuntimeException occurs.
    // If the exception is handled - e.g. in a try-catch - then
    // status.setRollbackOnly() must
    // be called manually in the catch block to trigger a roll-back.
    // But we use a Runnable here so status can not be passed to it...
    // () -> { ... } Runnable
    // () -> 42 Supplier
    // (s) -> System.out.println(s) Consumer

    // The implementation of the PlatformTransactionManager is needed to fix the
    // following error, occuring when executing code in a sepparate thread other
    // than the main thread of Spring ( Spring's request thread): Losing transaction
    // management by running DB code outside of Spring's request thread.

    // Exception in thread "pool-2-thread-1"
    // org.hibernate.LazyInitializationException: failed to lazily initialize a
    // collection of role: server.filestorm.model.entity.Directory.chunks: could not
    // initialize proxy - no Session

    // docs of TransactionOperations.execute and
    // TransactionOperations.executeWithoutResult
    // https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/support/TransactionOperations.html
}
