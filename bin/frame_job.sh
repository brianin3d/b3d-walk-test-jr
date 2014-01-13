#!/bin/bash	
########################################################################
#
# Usage: frame_job.sh inkscape.svg
#
# This script assumes the layers with names like "back..." and "bg.." are 
# background layers which should be rendered on every frame.
# 
# The naming conventions for the frames should be either "frame..." or "layer..."
#
# The naming is not case sensitive.
#
# The first group of numbers in the name are taken to be the frame number. Frame #1
# could be either "frame1" or "frame_1" or "frame_1_so_other_notes_or_#3"
# 
# The script does a bad job of trying to guess a delay. You can override it by 
# export DELAY variable in the shell you run it from.
#
# If you export KEEP_TMP_FILES, it will do just that as well.
#
# Oh... you need xsltproc and inkscape installed and on the path. Also imagemagick for 
# the animated gif.
#
# If everything works right, you will have a file called "test.gif" and "test-{timestamped}.gif"
# in pwd.
#
# Good luck
#
# Thx to http://daniel-albuschat.blogspot.com/2013/03/export-layers-from-svg-files-to-png.html
#
# @author Brian Hammond
#
########################################################################

_frame_job_xsl_begin() {
cat << EOM
<?xml version="1.0" encoding="UTF-8"?>
	<xsl:stylesheet
		version="1.0"
		xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:svg="http://www.w3.org/2000/svg"
		xmlns:inkscape="http://www.inkscape.org/namespaces/inkscape"
	>
EOM
}

_frame_job_xsl_end() {
cat << EOM
	</xsl:stylesheet>
EOM
}

_frame_job_list_xsl_layers() {
	_frame_job_xsl_begin
cat << EOM
	<xsl:output method="text" indent="no" encoding="UTF-8"/>

    <xsl:template match="svg:g[@inkscape:groupmode='layer']">
	    <xsl:value-of select="@inkscape:label"/>
<xsl:text>
</xsl:text>
	</xsl:template>

	<xsl:template match="text()"/>
EOM
	_frame_job_xsl_end
}

_frame_job_filter_xsl_layers() {
	local layer

	_frame_job_xsl_begin

cat << EOM
	<xsl:template match="@*|node()">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()"/>
		</xsl:copy>
	</xsl:template>
EOM


	for layer in ${*} ; do 
cat << EOM
		<xsl:template match="svg:g[@inkscape:label='${layer}']"/>
EOM
	done

	_frame_job_xsl_end
}

_frame_job_run() {
	local x=${1} ; shift
	_frame_job_${x} | xsltproc - ${*}
}

_frame_job_list_layers() {
	_frame_job_run list_xsl_layers ${*}
}

_frame_job_only_once() {
	tr ' ' '\n' | sort | uniq -c | expand | sed 's,^ *,,' | grep -v '^2 ' | cut -f2 -d' ' | xargs
}
		
_frame_job_main_filter() {
   local filename=${1}; shift

	_frame_job_filter_xsl_layers ${*} | xsltproc - ${filename}
}

_frame_job_usage() {
cat << EOM
usage: frame_job.sh inkscape.svg

export DELAY to override the amount of time per image
EOM
}

_frame_job_main() {
	# TODO: getopts
	if [ 1 != ${#} ] || [ ! -f ${1} ] ; then
		_frame_job_usage
		return 1
	fi

	####
	# get the list of layers and split them into either frame
	# layers or background layers based on naming conventions

	local all_layers=$( _frame_job_list_layers ${*} )
	local bg_layers=$( echo ${all_layers} | tr ' ' '\n' | egrep -i 'bg|back' )
	local frame_layers=$( echo ${all_layers} | tr ' ' '\n' | egrep -i '^(frame|layer)' | xargs )
	local frame_count=$( echo ${frame_layers} | wc -w  | expand | tr -d ' ' )

	echo "----------------------------------------------------------------------------"
	echo "frame layers: ${frame_layers}"
	echo "bg    layers: ${bg_layers}"
	echo "----------------------------------------------------------------------------"

	####
	# time to filter and render all the background layers and each frame layer separately

	local tmp_file=/tmp/.${USER}.frame_job.${$}
	local file_list=""
	local frame_layer
	for frame_layer in ${frame_layers} ; do
		#pull out the first set of numeric characters for the frame name...
		local frame=$( echo ${frame_layer} | sed 's,^[^0-9]*,,;s,[^0-9].*$,,' | awk '{ printf( "%04d\n", $1 ); }' )
		frame_count=$( echo ${frame} | sed 's,^0*,,' )

		# make the list of layers we want to filter out
		local trash=$( echo ${frame_layer} ${bg_layers} ${all_layers} | _frame_job_only_once )
		
		echo rendering ${frame} using ${frame_layer} 
		_frame_job_main_filter ${1} ${trash} > ${tmp_file}-frame-${frame}.svg || break
		local png_filename=${tmp_file}-${frame}.png
		inkscape ${tmp_file}-frame-${frame}.svg --export-png=${png_filename} || break
		file_list="${file_list} ${png_filename}"
	done

	####
	# make the animated image

	local elapsed_time=${ELAPSED_TIME-1000}
	local delay
	let delay=${elapsed_time}/${frame_count}

	if [ "" != "${DELAY}" ] ; then
		delay=${DELAY}
	fi

	echo "----------------------------------------------------------------------------"
	echo "elapsed time: ${elapsed_time}"
	echo "frame  count: ${frame_count}"
	echo "ms per frame: ${delay}"
	echo "----------------------------------------------------------------------------"

	local now=$( date +"%Y-%m-%d+%H-%M-%S" )
	local gifname=$( basename ${1} | sed "s,.svg$,-${now}.gif," )
	convert -delay ${delay} -loop 0 -alpha set -dispose previous ${file_list} ${gifname} 
	cp ${gifname} test.gif

	echo output should be in ${PWD}/${gifname} and ${PWD}/test.gif

	####
	# cleanup	
	if [ "" != "${KEEP_TMP_FILES}" ] ; then
		rm -f ${tmp_file}*
	fi
}

_frame_job_main ${*}
