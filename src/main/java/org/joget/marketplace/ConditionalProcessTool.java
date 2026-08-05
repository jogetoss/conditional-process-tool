package org.joget.marketplace;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.ApplicationPlugin;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.plugin.base.Plugin;
import org.joget.plugin.base.PluginManager;
import org.joget.plugin.property.model.PropertyEditable;
import org.joget.workflow.model.WorkflowAssignment;

public class ConditionalProcessTool extends DefaultApplicationPlugin{

    @Override
    public String getName() {
        return "Conditional Process Tool";
    }

    @Override
    public String getVersion() {
        return "9.0.1";
    }

    @Override
    public String getDescription() {
        return "Enable the use of process tool with condition";
    }
    
    @Override
    public String getLabel() {
        return "Conditional Process Tool";
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }
    
    @Override
    public String getPropertyOptions() {
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        String appId = appDef.getId();
        String appVersion = appDef.getVersion().toString();
        Object[] arguments = new Object[]{appId, appVersion};
        String json = AppUtil.readPluginResource(getClass().getName(), "/properties/ConditionalProcessTool.json", arguments, true, "messages/ConditionalProcessTool");
        return json;
    }

    @Override
    public Object execute(Map properties) {
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        boolean debugMode = Boolean.parseBoolean((String)properties.get("debug"));
        
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        
        if(debugMode){
            LogUtil.info(getClass().getName(), "Executing Conditional Process Tool");
        }
        
        String delayString = (String)properties.get("delay");

        int delayInt = 0;
        if(delayString == null){
            delayString = "0";
        }
        if(delayString.equalsIgnoreCase("true")){
            delayInt = 1;
        }else if(delayString.equalsIgnoreCase("false")){
            delayInt = 0;
        }else{
            delayInt = Integer.parseInt(delayString);
        }
        
        int delay = delayInt;
        
        try{
            String condition = (String)properties.get("condition");

            if(debugMode){
                LogUtil.info(getClass().getName(), "Evaluating condition: " + condition);
            }

            Object evalResult = AppPluginUtil.executeScript(condition, new HashMap());
            String output = (evalResult != null) ? evalResult.toString().trim() : "false";

            if(debugMode){
                LogUtil.info(getClass().getName(), "Result: " + output);
            }

            if("true".equalsIgnoreCase(output)){
                //executes the process tool plugin
                Object objProcessTool = properties.get("processTool");
                if (objProcessTool != null && objProcessTool instanceof Map) {
                    Map fvMap = (Map) objProcessTool;
                    if (fvMap != null && fvMap.containsKey("className") && !fvMap.get("className").toString().isEmpty()) {
                        String className = fvMap.get("className").toString();
                        ApplicationPlugin p = (ApplicationPlugin)pluginManager.getPlugin(className);

                        Map propertiesMap = new HashMap(properties);//(Map)fvMap.get("properties");
                        propertiesMap.putAll(AppPluginUtil.getDefaultProperties((Plugin) p, (Map) fvMap.get("properties"), (AppDefinition) properties.get("appDef"), (WorkflowAssignment) properties.get("workflowAssignment")));
                        ApplicationPlugin appPlugin = (ApplicationPlugin) p;

                        if (appPlugin instanceof PropertyEditable) {
                            ((PropertyEditable) appPlugin).setProperties(propertiesMap);
                        }

                        if(debugMode){
                            LogUtil.info(getClass().getName(), "Executing tool: " + className);
                        }

                        AppUtil.setCurrentAppDefinition(appDef);
                        Object result = appPlugin.execute(propertiesMap);

                        if(debugMode){
                            if(result != null){
                                LogUtil.info(getClass().getName(), "Executed tool: " + className + " - " + result.toString());
                            }else{
                                LogUtil.info(getClass().getName(), "Executed tool: " +  className);
                            }
                        }
                    }
                }
            }
        }catch(Exception ex){
            LogUtil.error(getClass().getName(), ex, "Condition evaluation error");
        }

        if(delay > 0){
            try {
                Thread.sleep(delay * 1000);
            } catch (InterruptedException ex) {
                LogUtil.info(getClass().getName(), "Sleep for  [" + delay + "] seconds.");
            }
        }
        
        return null;
    }
    
}
