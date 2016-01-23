On-Call Manager Plugin
=================================

Below is an example of a configured entry for this resource in the config.yaml file "resources" section.

```
resources:
  - name: PagerDuty
    className: com.labs2160.oncall.ctr.PagerDutyProvider
    configuration:
      apiUri: "https://batman.pagerduty.com/api/v1"
      apiKey: "SecretKey"
      scheduleID: "SecretID"

  - name: OnCallDB
    className: com.labs2160.oncall.ctr.DatabaseProvider
    configuration:
      DBDir: "/tmp/oncallDB/"
```
