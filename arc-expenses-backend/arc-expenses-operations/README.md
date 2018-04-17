# eic-operations

How to setup your very own eInfraCentral instance:

1. Get the code
    ```bash
    git clone https://github.com/eInfraCentral/eic-operations.git
    cd eic-operations
    ./clone.sh
    ```
2. Install site dependencies, then set up nginx, and analytics
    ```bash
    ./prod.sh install
    ./sites.sh setup
    ./anal.sh setup
    ```
3. Fill in eic-docker/secret.env with your instance's secrets (look at the template there for guidance)
4. Build & raise backend
    ```bash
    ./mcip.sh
    ./tom.sh up
    ```
5. Populate repository in some way (either POST your own, or use `./dat.sh add resources` for an older snapshot of eic's catalogue)
    ```bash
    ./dat.sh add types
    ./dat.sh add resources
    ./tom.sh restart
    ```
6. Build & raise frontend, and analytics:
    ```bash
    ./prod.sh up
    ./anal.sh up
    ```
    
* To enable maintenance page:
    ```bash
    ./sites.sh enable maintenance
    ```
* To disable maintenance page:
    ```bash
    ./sites.sh enable proxy
    ```
