package de.unijena.bioinf.ms.middleware.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.ant("/api/**"))
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("Sirius Nightsky Middleware API")
                .description("Sirius Nightsky Middleware API")
                .termsOfServiceUrl("https://bio.informatik.uni-jena.de/software/sirius/")
                .license("GNU General Public License v3.0")
                .licenseUrl("https://github.com/boecker-lab/sirius_frontend/blob/release-4.4/LICENSE.txt")
                .version("0.9")
                .build();
    }
}
