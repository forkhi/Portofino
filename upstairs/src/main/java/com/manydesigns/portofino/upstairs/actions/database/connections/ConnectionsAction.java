package com.manydesigns.portofino.upstairs.actions.database.connections;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.manydesigns.elements.ElementsThreadLocals;
import com.manydesigns.elements.Mode;
import com.manydesigns.elements.annotations.*;
import com.manydesigns.elements.configuration.CommonsConfigurationUtils;
import com.manydesigns.elements.forms.Form;
import com.manydesigns.elements.forms.FormBuilder;
import com.manydesigns.elements.messages.RequestMessages;
import com.manydesigns.elements.messages.SessionMessages;
import com.manydesigns.elements.util.FormUtil;
import com.manydesigns.portofino.model.database.*;
import com.manydesigns.portofino.model.database.platforms.DatabasePlatform;
import com.manydesigns.portofino.pageactions.AbstractPageAction;
import com.manydesigns.portofino.persistence.Persistence;
import com.manydesigns.portofino.security.RequiresAdministrator;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.json.JSONStringer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Alessio Stalla - alessiostalla@gmail.com
 */
@RequiresAuthentication
@RequiresAdministrator
public class ConnectionsAction extends AbstractPageAction {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionsAction.class);

    @Autowired
    protected Persistence persistence;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ConnectionProviderSummary> list() {
        List<ConnectionProviderSummary> list = new ArrayList<>();
        persistence.getModel().getDatabases().forEach(database -> {
            ConnectionProvider connectionProvider = database.getConnectionProvider();
            list.add(new ConnectionProviderSummary(
                    database.getDatabaseName(), connectionProvider.getDescription(), connectionProvider.getStatus()));
        });
        return list;
    }

    @GET
    @Path("{databaseName}")
    @Produces(MediaType.APPLICATION_JSON)
    public String describeConnection(@PathParam("databaseName") String databaseName) throws Exception {
        ConnectionProvider connectionProvider = persistence.getConnectionProvider(databaseName);
        ConnectionProviderDetail cp = new ConnectionProviderDetail(connectionProvider);
        Form form = new FormBuilder(ConnectionProviderDetail.class).build();
        form.readFromObject(cp);
        if (ConnectionProvider.STATUS_CONNECTED.equals(connectionProvider.getStatus())) {
            //TODO configureDetected();
        }
        JSONStringer js = new JSONStringer();
        js.object();
        FormUtil.writeToJson(form, js);
        js.key("schemas").array();
        Connection conn = connectionProvider.acquireConnection();
        logger.debug("Reading database metadata");
        DatabaseMetaData metadata = conn.getMetaData();
        List<String[]> schemaNamesFromDb =
                connectionProvider.getDatabasePlatform().getSchemaNames(metadata);
        List<Schema> selectedSchemas = connectionProvider.getDatabase().getSchemas();
        for(String[] schemaName : schemaNamesFromDb) {
            boolean selected = false;
            for(Schema schema : selectedSchemas) {
                if(schemaName[1].equalsIgnoreCase(schema.getSchema())) {
                    selected = true;
                    break;
                }
            }
            js.object();
            js.key("catalog").value(schemaName[0]);
            js.key("name").value(schemaName[1]);
            js.key("selected").value(selected);
            js.endObject();
        }
        js.endArray();
        js.endObject();
        return js.toString();
    }

    @PUT
    @Path("{databaseName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response saveConnection(@PathParam("databaseName") String databaseName) {
        ConnectionProvider connectionProvider = persistence.getConnectionProvider(databaseName);
        ConnectionProviderDetail cp = new ConnectionProviderDetail(connectionProvider);
        Form form = new FormBuilder(ConnectionProviderDetail.class).build();
        form.readFromObject(cp);
        form.readFromRequest(context.getRequest());
        //TODO schema select
        if(form.validate()) {
            form.writeToObject(cp);
            try {
                connectionProvider.init(persistence.getDatabasePlatformsRegistry());
                persistence.initModel();
                persistence.saveXmlModel();
                return Response.ok(form).build();
            } catch (Exception e) {
                String msg = "Cannot save model: " + ExceptionUtils.getRootCauseMessage(e);
                logger.error(msg, e);
                RequestMessages.addErrorMessage(msg);
                return Response.serverError().entity(form).build();
            }
        }
        return Response.serverError().entity(form).build();
    }

    @POST
    @Path("{databaseName}/:synchronize")
    public void synchronize(@PathParam("databaseName") String databaseName) throws Exception {
        persistence.syncDataModel(databaseName);
        persistence.initModel();
        persistence.saveXmlModel();
    }

    public static class ConnectionProviderSummary {

        public String databaseName;
        public String status;
        public String description;

        public ConnectionProviderSummary(String databaseName, String description, String status) {
            this.databaseName = databaseName;
            this.status = status;
            this.description = description;
        }
    }

    @JsonClassDescription
    public static class ConnectionProviderDetail {
        public static final String copyright =
                "Copyright (C) 2005-2017 ManyDesigns srl";

        @JsonIgnore
        protected Database database;
        protected String databasePlatform;
        @JsonIgnore
        protected JdbcConnectionProvider jdbcConnectionProvider;
        @JsonIgnore
        protected JndiConnectionProvider jndiConnectionProvider;

        public ConnectionProviderDetail(ConnectionProvider connectionProvider) {
            this.database = connectionProvider.getDatabase();
            DatabasePlatform databasePlatform = connectionProvider.getDatabasePlatform();
            this.databasePlatform = databasePlatform.getClass().getName();
            if(connectionProvider instanceof JdbcConnectionProvider) {
                jdbcConnectionProvider = (JdbcConnectionProvider) connectionProvider;
            } else if(connectionProvider instanceof JndiConnectionProvider) {
                jndiConnectionProvider = (JndiConnectionProvider) connectionProvider;
            } else {
                throw new IllegalArgumentException("Invalid connection provider type: " + connectionProvider);
            }
        }

        public void setDatabaseName(String databaseName) {
            database.setDatabaseName(databaseName);
        }

        @Updatable(false)
        @Required(true)
        public String getDatabaseName() {
            return database.getDatabaseName();
        }

        public void setTrueString(String trueString) {
            database.setTrueString(StringUtils.defaultIfEmpty(trueString, null));
        }

        public String getTrueString() {
            return database.getTrueString();
        }

        public void setFalseString(String falseString) {
            database.setFalseString(StringUtils.defaultIfEmpty(falseString, null));
        }

        public String getFalseString() {
            return database.getFalseString();
        }

        @FieldSize(50)
        @Required
        public String getDriver() {
            return jdbcConnectionProvider.getDriver();
        }

        public void setDriver(String driver) {
            jdbcConnectionProvider.setDriver(driver);
        }

        @FieldSize(100)
        @Required
        @Label("connection URL")
        public String getUrl() {
            return jdbcConnectionProvider.getActualUrl();
        }

        public void setUrl(String url) {
            jdbcConnectionProvider.setActualUrl(url);
        }

        public String getUsername() {
            return jdbcConnectionProvider.getActualUsername();
        }

        public void setUsername(String username) {
            jdbcConnectionProvider.setActualUsername(username);
        }

        @Password
        public String getPassword() {
            return jdbcConnectionProvider.getActualPassword();
        }

        public void setPassword(String password) {
            jdbcConnectionProvider.setActualPassword(password);
        }

        @Required
        public String getJndiResource() {
            return jndiConnectionProvider.getJndiResource();
        }

        public void setJndiResource(String jndiResource) {
            jndiConnectionProvider.setJndiResource(jndiResource);
        }

        @Label("Hibernate dialect (leave empty to use default)")
        public String getHibernateDialect() {
            if(jdbcConnectionProvider != null) {
                return jdbcConnectionProvider.getHibernateDialect();
            } else if(jndiConnectionProvider != null) {
                return jndiConnectionProvider.getHibernateDialect();
            } else {
                return null;
            }
        }

        public void setHibernateDialect(String dialect) {
            if(jdbcConnectionProvider != null) {
                jdbcConnectionProvider.setHibernateDialect(dialect);
            } else if(jndiConnectionProvider != null) {
                jndiConnectionProvider.setHibernateDialect(dialect);
            } else {
                throw new Error("Misconfigured");
            }
        }

        public String getErrorMessage() {
            if(jdbcConnectionProvider != null) {
                return jdbcConnectionProvider.getErrorMessage();
            } else if(jndiConnectionProvider != null) {
                return jndiConnectionProvider.getErrorMessage();
            } else {
                return null;
            }
        }

        public String getStatus() {
            if(jdbcConnectionProvider != null) {
                return jdbcConnectionProvider.getStatus();
            } else if(jndiConnectionProvider != null) {
                return jndiConnectionProvider.getStatus();
            } else {
                return null;
            }
        }

        public Date getLastTested() {
            if(jdbcConnectionProvider != null) {
                return jdbcConnectionProvider.getLastTested();
            } else if(jndiConnectionProvider != null) {
                return jndiConnectionProvider.getLastTested();
            } else {
                return null;
            }
        }

    }



}