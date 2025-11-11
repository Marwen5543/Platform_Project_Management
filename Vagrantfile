Vagrant.configure("2") do |config|
  config.vm.box = "bento/ubuntu-22.04"
  config.vm.box_download_options = {"ssl-no-revoke" => true}
  
  # Increase boot timeout
  config.vm.boot_timeout = 1200
  config.ssh.connect_timeout = 60
  # --- CORRECT WAY TO INCREASE DISK SIZE (requires 'vagrant-disksize' plugin) ---
  config.vm.disk :disk, size: "60GB", primary: true
  
  # Network configuration
  config.vm.network "private_network", ip: "192.168.50.4"
  config.vm.network "forwarded_port", guest: 8080, host: 8082
  config.vm.network "forwarded_port", guest: 32246, host: 32246
  config.vm.network "forwarded_port", guest: 33337, host: 33337
  config.vm.network "forwarded_port", guest: 30082, host: 30082
  config.vm.network "forwarded_port", guest: 30000, host: 30000
  config.vm.network "forwarded_port", guest: 8083, host: 8084
  config.vm.network "forwarded_port", guest: 31000, host: 31000
  config.vm.network "forwarded_port", guest: 8081, host: 8081
  config.vm.network "forwarded_port", guest: 8761, host: 8761   
  config.vm.network "forwarded_port", guest: 8085, host: 8085
  config.vm.network "forwarded_port", guest: 30231, host: 30231
  config.vm.network "forwarded_port", guest: 32237, host: 32237
  config.vm.network "forwarded_port", guest: 30002, host: 30002

  # VirtualBox provider
  config.vm.provider "virtualbox" do |vb|
    vb.memory = "16384"  # 16GB to reduce host contention
    vb.cpus = "8"       # 8 cores for stability
    vb.customize ["modifyvm", :id, "--nested-hw-virt", "on"]
    vb.customize ["modifyvm", :id, "--ioapic", "on"]
    vb.customize ["modifyvm", :id, "--paravirtprovider", "default"]
    vb.customize ["modifyvm", :id, "--cpuexecutioncap", "100"]
    vb.customize ["modifyvm", :id, "--natdnsproxy1", "on"]
    vb.customize ["modifyvm", :id, "--natdnshostresolver1", "on"]
    vb.gui = true  # Enable GUI for boot debugging
    
  end

  # Ensure synced folder permissions
  config.vm.synced_folder ".", "/vagrant", type: "virtualbox", mount_options: ["uid=1000", "gid=1000", "fmode=755"]

  # Initial setup: Install Ansible
  config.vm.provision "shell", inline: <<-SHELL
    echo "=== Starting initial provisioning $(date) ==="
    sudo apt-get update
    sudo apt-get install -y curl ansible
# --- ADDED: Install Helm ---
    echo "=== Installing Helm... ==="
    curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3
    chmod 700 get_helm.sh
    ./get_helm.sh
    echo "=== Helm installed successfully. ==="
# Your existing setup commands

    sudo mkdir -p /etc/systemd/resolved.conf.d
    sudo mkdir -p /home/vagrant/.kube
    sudo chown vagrant:vagrant /home/vagrant/.kube
    # Debug: Verify synced files
    echo "Contents of /vagrant:"
    ls -l /vagrant
  SHELL

  # Run Ansible playbook
  config.vm.provision "ansible_local" do |ansible|
    ansible.playbook = "playbook.yml"
    ansible.become = true
    ansible.compatibility_mode = "2.0"
  end
end