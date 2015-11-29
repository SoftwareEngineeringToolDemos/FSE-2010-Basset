1. When you run the "vagrant up" command, the box automatically navigates to "/home/vagrant/Desktop/jpf/jpf-actor" folder
   and runs the command "bin/jpf gov.nasa.jpf.actor.Basset pi.Driver 3".
2. The jpf-core folder can be found in the directory "/home/vagrant/Desktop/jpf/jpf-core".
3. The vagrant software automatically installs ant and runs the build in both jpf-core as well as jpf-actor folder.
4. It places the site.properties file in the "/home/vagrant/Desktop/jpf" and "/home/vagrant/.jpf" folder.
5. Since the command automatically runs in the terminal, you may observe the output of the command and understand the various parameters.
6. For further details, please refer the youtube video on desktop.
7. You may also refer the website :http://mir.cs.illinois.edu/basset/ and read the papers related to Basset . For example : http://mir.cs.illinois.edu/basset/pubs/basset-fse2010.pdf


Acknowledgement: Thanks to Steve Lauterburg for the inputs which were crucial for the setup.

References: 

1. http://mir.cs.illinois.edu/basset/pubs/basset-fse2010.pdf
2. http://mir.cs.illinois.edu/basset/