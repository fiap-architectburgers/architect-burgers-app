package com.example.gomesrodris.archburgers.adapters.datasource;


import com.mongodb.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Scope("singleton")
public class MongoDatabaseConnection {

    private Environment environment;

    @Autowired
    public MongoDatabaseConnection(Environment environment) {
        this.environment = environment;
    }

    @NotNull
    public MongoClientSettings getMongoClientSettings() {
        MongoClientSettings settings;
        String connectionString = this.environment.getProperty("archburgers.datasource-mongodb.uri");

        if (connectionString == null) {
            throw new IllegalStateException("datasource-mongodb.uri env is missing");
        }

        ServerApi serverApi = ServerApi.builder()
                .version(ServerApiVersion.V1)
                .build();

        settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .serverApi(serverApi)
                .build();
        return settings;
    }

    public String getDatabase(){
        return environment.getProperty("archburgers.datasource-mongodb.database");
    }
}
