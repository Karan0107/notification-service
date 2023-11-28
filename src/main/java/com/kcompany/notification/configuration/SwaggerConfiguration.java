package com.kcompany.notification.configuration;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Configuration
public class SwaggerConfiguration {

    @Bean
    public GroupedOpenApi controllerApi() {
        return GroupedOpenApi.builder()
                .group("com.kcompany")
                .packagesToScan("com.kcompany") // Specify the package to scan
                .build()
                .addAllOpenApiCustomizer(setOpenApiCustomizer());
    }

    public Collection<? extends OpenApiCustomizer> setOpenApiCustomizer() {
        Collection<OpenApiCustomizer> objects = new ArrayList<>();
        OpenApiCustomizer openApiCustomizer = openApi -> {
            openApi.setOpenapi("3.0.1");

            Info info = new Info();
            info.setTitle("Notification Service");
            info.setVersion("1.0");
            openApi.info(info);

            List<Server> serverList = new ArrayList<>(1);
            Server server = new Server();
            server.setUrl("http://localhost:8989");
            server.setDescription("");
            serverList.add(server);
            openApi.servers(serverList);

//            Append new server in existing one.
//            List<Server> servers = openApi.getServers();
//            Server server = new Server();
//            server.setUrl("http://localhost:8989");
//            server.setDescription("");
//            servers.add(server);
//            openApi.servers(servers);
        };
        objects.add(openApiCustomizer);
        return objects;
    }
}
