[中文](README.md) / English

---

## About

**neatlogic-alert** is the Alert Center module, mainly used for centralized collection, processing, and visualization of
alerts from multiple sources.  
**neatlogic-alert** cannot be deployed or built independently. For build and deployment instructions, please refer to
the documentation of  
[neatlogic-itom-all](../../../neatlogic-itom-all/blob/develop3.0.0/README.md).

## Architecture Diagram

![img_1.png](README_IMAGES/img_1.png)

## Main Features

### Alert Type Management

Configure alert types and adaptors.

- One alert type can support multiple adaptors
- Adaptors must implement the interfaces defined in  
  [neatlogic-alert-plugin-base](https://gitee.com/neat-logic/neatlogic-alert-plugin-base.git)

![img_2.png](README_IMAGES/img_2.png)

### Alert Event Management

- Currently supported events include: alert ingestion (start event), create alert, alert convergence, sub-alert join,
  sub-alert removal, update alert status, close alert, open alert, and delete alert. More events may be added as needed
  in the future.
- Currently supported plugins include: create alert, assign handler, condition judgment, scheduled execution, email
  notification, integration invocation, etc. More event plugins may be added in the future, and custom event plugins can
  also be implemented in custom projects.

### Extended Attribute Management

- Supports four data types: number, text, enumeration, and date/time
- Enumeration types automatically persist data for use as search conditions

![img.png](README_IMAGES/img_3.png)

### Alert Views

Alert views are used to configure alert lists visible to different roles.

- Supports conditional filtering of alert data
- Supports defining visible fields
- Supports authorization to restrict usage scope

![img.png](README_IMAGES/img_4.png)

### Event-Driven Lifecycle Management

Complex lifecycle management is achieved through event-driven orchestration and coupling.

- Supports 10 types of events, with a framework that provides strong extensibility for event expansion
- Provides more than 15 event plugins, supporting scenarios such as alert status changes, escalation, assignment,
  delayed execution, condition evaluation, tagging, and closure. The framework offers strong extensibility for plugin
  expansion.

![img.png](README_IMAGES/img5.png)

### Alert Topology (Commercial Edition)

Alerts are visualized using topology graphs.

- Supports multiple types of topology elements
- Supports binding multiple alert data sources to topology elements

![img.png](README_IMAGES/img_7.png)
![img.png](README_IMAGES/img_6.png)

### Large Model Integration (Commercial Edition)

Alert content is analyzed using large language models.

- Supports both OpenAI and Ollama interfaces

![img.png](README_IMAGES/img_8.png)

## Feature List

<table>
    <tr>
        <td>ID</td>
        <td>Category</td>
        <td>Feature</td>
        <td>Description</td>
        <td>Open Source</td>
    </tr>
    <tr>
        <td>1</td>
        <td>Data Ingestion</td>
        <td>Multiple Data Source Ingestion</td>
        <td>Supports ingesting alert data from multiple sources such as message queues, HTTP APIs, files, and monitoring systems, with unified aggregation and processing.</td>
        <td>✅</td>
    </tr>
    <tr>
        <td>2</td>
        <td></td>
        <td>Custom Adaptors</td>
        <td>Supports custom adaptors to convert alerts with different formats and field structures into a unified format, reducing integration complexity.</td>
        <td>✅</td>
    </tr>
    <tr>
        <td>3</td>
        <td></td>
        <td>Adaptor Hot Loading</td>
        <td>Adaptors support hot updates and dynamic loading, allowing new alert source types to be added without restarting the system.</td>
        <td>✅</td>
    </tr>
    <!-- Alert Processing -->
    <tr>
        <td>4</td>
        <td>Processing Workflow</td>
        <td>Event-Driven Model</td>
        <td>The alert lifecycle is managed through an event-driven model. All create, update, and delete operations generate events that drive state transitions and processing actions.</td>
        <td>✅</td>
    </tr>
    <tr>
        <td>5</td>
        <td></td>
        <td>Event Plugin Mechanism</td>
        <td>Built-in support for more than 10 event plugins, enabling complex business logic such as escalation, assignment, tagging, field modification, and third-party API invocation.</td>
        <td>✅</td>
    </tr>
    <tr>
        <td>6</td>
        <td></td>
        <td>Custom Event Plugins</td>
        <td>Developers can provide new event plugins to extend processing logic and meet special business requirements.</td>
        <td>✅</td>
    </tr>
    <tr>
        <td>7</td>
        <td>Customization</td>
        <td>Custom Status Workflow</td>
        <td>Supports defining alert status workflows, such as New → In Progress → Resolved → Closed, configurable according to business needs.</td>
        <td>✅</td>
    </tr>
    <tr>
        <td>8</td>
        <td></td>
        <td>Custom Severity Levels</td>
        <td>Supports defining alert severity levels, including count, labels, and colors, for display and filtering purposes.</td>
        <td>✅</td>
    </tr>
    <tr>
        <td>9</td>
        <td></td>
        <td>Custom Extended Fields</td>
        <td>Allows adding extended fields to alerts, including text, numbers, lists, and time types, to record specific business attributes.</td>
        <td>✅</td>
    </tr>
    <tr>
        <td>10</td>
        <td></td>
        <td>Dynamic Field Display and Sorting</td>
        <td>All list and detail pages support configuration of field visibility, ordering, mandatory rules, and validation rules.</td>
        <td>✅</td>
    </tr>
    <tr>
        <td>11</td>
        <td>Policy Management</td>
        <td>Subscription Policies</td>
        <td>Configure alert subscription scopes by user, organization, or system, including conditions such as severity, source, object, and keywords.</td>
        <td>❌</td>
    </tr>
    <tr>
        <td>12</td>
        <td></td>
        <td>Suppression Policies</td>
        <td>Supports suppression rules based on time ranges, sources, severity levels, and regular expressions to reduce alert noise.</td>
        <td>❌</td>
    </tr>
    <tr>
        <td>13</td>
        <td>AI Analysis</td>
        <td>Intelligent Alert Analysis</td>
        <td>The commercial edition supports integration with large AI models to perform alert classification, pattern recognition, root cause analysis, and provide handling suggestions.</td>
        <td>❌</td>
    </tr>
    <tr>
        <td>14</td>
        <td>Integration</td>
        <td>Open APIs</td>
        <td>Provides comprehensive APIs for ingestion, querying, processing, and statistics, enabling integration with CMDBs, automation platforms, and monitoring systems.</td>
        <td>✅</td>
    </tr>
<tr>
    <td>15</td>
    <td>Alert Suppression</td>
    <td>Custom Suppression Policies</td>
    <td>Supports custom condition-based and time-based suppression of event plugin execution.</td>
    <td>❌</td>
</tr>
<tr>
    <td>16</td>
    <td>Alert Subscription</td>
    <td>Custom Subscriptions</td>
    <td>Supports subscribing to specific alerts based on custom conditions, with multiple subscription plugins such as email and third-party integrations.</td>
    <td>❌</td>
</tr>
</table>