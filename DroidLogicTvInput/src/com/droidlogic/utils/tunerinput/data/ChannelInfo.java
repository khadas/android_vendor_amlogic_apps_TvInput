package com.droidlogic.utils.tunerinput.data;

import java.util.Arrays;

public final class ChannelInfo {

	public final String number;
	public final String name;
	public final String logoUrl;
	public final int originalNetworkId;
	public final int transportStreamId;
	public final String inputId;
	public final int serviceId;
	public final int videoWidth;
	public final int videoHeight;
	public final int type;
	public final int serviceType;
	public final int frequency;
	public final int bandwidth;
	public final int videoPID;
	public final int videoFormat;
	public final int audioPIDs[];
	public final int audioFormats[];
	public final String audioLangs[];
	public final int pcrPID;
	public int videoStd;
	public int audioStd;
	public final int isAutoStd;
	public final int fineTune;
	public int audioTrackIndex;
	public int audioCompensation;

	public ChannelInfo(String number,
	                    String name,
	                    String logoUrl,
	                    int originalNetworkId,
	                    int transportStreamId,
	                    String inputId,
	                    int serviceId,
	                    int videoWidth,
	                    int videoHeight,
	                    int type,
	                    int serviceType,
	                    int frequency,
	                    int bandwidth,
	                    int videoPID,
	                    int videoFormat,
	                    int audioPIDs[],
	                    int audioFormats[],
	                    String audioLangs[],
	                    int pcrPID,
	                    int vStd,
	                    int aStd,
	                    int autoStd,
	                    int fine,
	                    int audioTrackIndex,
	                    int audioCompensation) {
		this.number = number;
		this.name = name;
		this.logoUrl = logoUrl;
		this.originalNetworkId = originalNetworkId;
		this.transportStreamId = transportStreamId;
		this.inputId = inputId;
		this.serviceId = serviceId;
		this.videoWidth = videoWidth;
		this.videoHeight = videoHeight;
		this.type = type;
		this.serviceType = serviceType;
		this.frequency = frequency;
		this.bandwidth = bandwidth;
		this.videoPID = videoPID;
		this.videoFormat = videoFormat;
		this.audioFormats = audioFormats;
		this.audioPIDs = audioPIDs;
		this.audioLangs = audioLangs;
		this.pcrPID = pcrPID;
		this.videoStd = vStd;
		this.audioStd = aStd;
		this.isAutoStd = autoStd;
		this.fineTune = fine;
		this.audioTrackIndex = audioTrackIndex;
		this.audioCompensation = audioCompensation;
	}

	public String toString(){
		return ""+number+" "+name+" logo:"+logoUrl+" onetworkid:"+originalNetworkId+" tsid:"+transportStreamId
			+" serviceid:"+serviceId+" type:"+type+" servicetype:"+serviceType
			+" freq:"+frequency+" bw:"+bandwidth
			+" vid:"+videoPID+" vfmt:"+videoFormat
			+" aids:"+Arrays.toString(audioPIDs)+" afmts:"+Arrays.toString(audioFormats)
			+" alangs:"+Arrays.toString(audioLangs)
			+" pcr:"+pcrPID;
	}
}


