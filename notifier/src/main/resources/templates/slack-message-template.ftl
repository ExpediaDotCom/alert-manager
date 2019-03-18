Hello!

You have one alert generated with below details.

<#if alert.labels?has_content>
Labels:
   <#list alert.labels?keys as prop>
      <#if prop?has_content>
         ${prop} = ${(alert.labels[prop])!""}
      </#if>
   </#list>
</#if>

<#if alert.annotations?has_content>

Annotations:
   <#list alert.annotations?keys as prop>
      <#if prop?has_content>
         ${prop} = ${(alert.annotations[prop])!""}
      </#if>
   </#list>
</#if>

<#if alert.expectedValue?has_content>

Expected Value: ${alert.expectedValue}
</#if>
<#if alert.observedValue?has_content>
Observed Value: ${alert.observedValue}
</#if>

<#if alert.generatorURL?has_content>
More details on alert can be accessed from <${alert.generatorURL}|here>
</#if>

You can access alert subscription rules from the <http://console|console>.
