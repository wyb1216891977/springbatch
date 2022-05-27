package com.baeldung.batch;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import java.io.File;
import java.io.IOException;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {

    @Autowired
    public JobBuilderFactory jobBuilderFactory;

    @Autowired
    public StepBuilderFactory stepBuilderFactory;
    
    @Value("${file.input}")
    private String fileInput;


    @Bean
    public FlatFileItemReader<Coffee> reader() {
        return new FlatFileItemReaderBuilder<Coffee>().name("coffeeItemReader")
            .resource(new ClassPathResource(fileInput))
            .delimited()
            .names(new String[] { "brand", "origin", "characteristics" })
            .fieldSetMapper(new BeanWrapperFieldSetMapper<Coffee>() {{
                setTargetType(Coffee.class);
             }})
            .build();
    }

    @Bean
    public CoffeeItemProcessor processor() {
        return new CoffeeItemProcessor();
    }

    @Bean
    public JdbcBatchItemWriter<Coffee> writer(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Coffee>().itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
            //.sql("INSERT INTO coffee (brand, origin, characteristics) VALUES (:brand, :origin, :characteristics)")
            .sql("UPDATE coffee SET origin=:origin WHERE brand=:brand")
            .dataSource(dataSource)
            .build();
    }
    @Bean
    public FlatFileItemWriter<Coffee> writer2() {
        File file=new File("src/main/resources/coffee-out.csv");    
        if (!file.exists()) {
        	try {
				file.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
   
        BeanWrapperFieldExtractor<Coffee> fieldExtractor = new BeanWrapperFieldExtractor<>();
        fieldExtractor.setNames(new String[] { "brand", "origin", "characteristics" }); //设置映射field
        fieldExtractor.afterPropertiesSet(); //参数检查
 
        DelimitedLineAggregator<Coffee> lineAggregator = new DelimitedLineAggregator<>();
        lineAggregator.setDelimiter(","); //设置输出分隔符
        lineAggregator.setFieldExtractor(fieldExtractor); //设置FieldExtractor处理器
 
        FlatFileItemWriter<Coffee> fileWriter = new FlatFileItemWriter<>();
        fileWriter.setLineAggregator(lineAggregator);
        fileWriter.setResource(new FileSystemResource("src/main/resources/coffee-out.csv")); //设置输出文件位置
        fileWriter.setName("outpufData");
 

        return fileWriter;

    }

    @Bean
    public Job importUserJob(JobCompletionNotificationListener listener, Step step1 ,Step step2) {
        return jobBuilderFactory.get("importUserJob")
            .incrementer(new RunIdIncrementer())
            .listener(listener)
            //.flow(step2())
            .start(step1)
            .next(jobDecider())
            .from(jobDecider()).on("CONTINUE").to(step1)
            .from(jobDecider()).on("COMPLETED").to(step2())
            .end()
            .build();
    }

    @Bean
    public Step step1(JdbcBatchItemWriter<Coffee> writer) {
        return stepBuilderFactory.get("step1")
            .<Coffee, Coffee> chunk(10)
            .reader(reader())
            .processor(processor())
            .writer(writer)
            .build();
    }

    @Bean
    public Step step2() {
        return stepBuilderFactory.get("step2")
            .<Coffee, Coffee> chunk(10)
            .reader(reader())
            .processor(processor())
            .writer(writer2())
            .build();
    }
    
    @Bean
    public JobExecutionDecider jobDecider(){
        return new JobDecider();
    }
}
