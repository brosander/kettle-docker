FROM cogniteev/oracle-java:java8

RUN useradd etlguy

RUN mkdir -p /home/etlguy
ADD data-integration.tar.gz /home/etlguy/
ADD config.xml /home/etlguy/
RUN chown -R etlguy:etlguy /home/etlguy

EXPOSE 8080 9001
CMD ["sudo", "-u", "etlguy", "/home/etlguy/data-integration/carte.sh", "/home/etlguy/config.xml"]
