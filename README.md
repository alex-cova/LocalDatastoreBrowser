# Local Datastore Browser
LDB Or **Local Datastore Browser** is the missing tool to work with the GCP Datastore emulator
<img width="932" alt="Screen Shot 2022-04-23 at 23 47 52" src="https://user-images.githubusercontent.com/25180512/164958431-29fc099d-11d5-41f2-b5b2-279bc2194e37.png">

## 1. Get Datastore Emulator Up & Running

datastore-compose.yaml

```
docker-compose -f datastore-compose.yaml up
```

```yaml
version: '3.8'
services:
datastore-local:
command: "gcloud beta emulators datastore start --project test-project --host-port 0.0.0.0:8081 --consistency 1.0"
image: "gcr.io/google.com/cloudsdktool/cloud-sdk:382.0.0-emulators"
container_name: datastore-local
ports:
- "8081:8081"
volumes:
- "./datastore:/root/.config/gcloud/emulators/datastore/WEB-INF/appengine-generated/"
working_dir: /home/datastore
```
## 2. Open LDB
   Java 17 or newer is requiered.
```
java - jar 'fileName.jar'
```

## 3. Set up your project-id
<img width="374" alt="Screen Shot 2022-04-23 at 23 48 16" src="https://user-images.githubusercontent.com/25180512/164958436-758d1f67-98f6-41b2-93c2-0469a15e771e.png">

## 4. Browse your data
<img width="932" alt="Screen Shot 2022-04-23 at 23 47 57" src="https://user-images.githubusercontent.com/25180512/164958440-f3c62e8a-1709-4e82-8004-132169197349.png">

## Why I made this & TODO List
I’ve been working with Datastore (Firestore mode) for a while, when one of the tests fails can be [tricky](https://cloud.google.com/datastore/docs/tools/emulator-export-import) to check what happened,
maybe I’m wrong, But despite that I’m kinda UI man, so enjoyed making this app in my free time, and it already saved me hours of work :).

**TODO**

- [ ] Paginator
- [x] Entity Viewer
- [ ] UI Components for filters
- [ ] Entity Editor
- [ ] Data Exporter/Importer
- [x] Kind properties viewer
- [ ] Clean up the code
- [ ] And much more…

---

This application is powered by OSS projects ❤️
