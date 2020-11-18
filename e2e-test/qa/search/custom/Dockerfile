ARG SEARCH_TAG=latest
FROM alfresco/search-services:2.0.0
LABEL creator="Paul Brodner" maintainer="Alfresco Search Services Team"

ARG SCRIPTS_FOLDER=

USER root
RUN echo " &" >> $DIST_DIR/solr/bin/search_config_setup.sh && \
    echo "bash -c \"find $DIST_DIR/scripts/ -maxdepth 1 -type f -executable -name '*.sh' -exec {} \\;\"" >> $DIST_DIR/solr/bin/search_config_setup.sh && \
    echo "bash -c \"tail -f $DIST_DIR/logs/solr.log\"" >> $DIST_DIR/solr/bin/search_config_setup.sh

USER solr
COPY ${SCRIPTS_FOLDER}/* ${DIST_DIR}/scripts/

# we need this, because we tail on it in the search_config_setup.sh (see above)
RUN touch ./logs/solr.log