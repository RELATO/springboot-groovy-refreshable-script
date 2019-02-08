package demo;

import java.util.concurrent.TimeUnit;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DelegatingIntroductionInterceptor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scripting.groovy.GroovyScriptFactory;
import org.springframework.scripting.support.RefreshableScriptTargetSource;
import org.springframework.scripting.support.ResourceScriptSource;

@SpringBootApplication
public class App {

	public static void main(String[] args) throws Exception{

		Evaluator evaluator = SpringApplication.run(App.class, args).getBean("evaluator", Evaluator.class);

		for (int i = 0; i < 20; i++) {

			System.out.printf("evaluated to %s%n", evaluator.evaluate(null));

			TimeUnit.SECONDS.sleep(1);
		}
	}

	@Bean
	public Evaluator evaluator(BeanFactory beanFactory) throws Exception {

		GroovyScriptFactory factory = new GroovyScriptFactory("classpath");
		factory.setBeanFactory(beanFactory);

		ResourceScriptSource script = new ResourceScriptSource(new ClassPathResource("GroovyEvaluator.groovy"));

		RefreshableScriptTargetSource rsts = new RefreshableScriptTargetSource(beanFactory, "ignored-bean-name", factory, script, false) {

			@Override
			protected Object obtainFreshBean(BeanFactory beanFactory, String beanName) {
				
				/*
				 * we ask the factory to create a new script bean directly instead
				 * asking the beanFactory for simplicity. 
				 */
				try {
					return factory.getScriptedObject(script);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		};
		rsts.setRefreshCheckDelay(1000L);

		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTargetSource(rsts);
		proxyFactory.setInterfaces(Evaluator.class);

		DelegatingIntroductionInterceptor introduction = new DelegatingIntroductionInterceptor(rsts);
		introduction.suppressInterface(TargetSource.class);
		proxyFactory.addAdvice(introduction);

		return (Evaluator) proxyFactory.getProxy();
	}
}
