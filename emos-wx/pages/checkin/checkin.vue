<template>
	<view>
		<camera device-position="front" flash="off" class="camera" @error="error" v-if="showCamera"></camera>
		<image mode="widthFix" class="image" :src="photoPath" v-if="showImage"></image>
		<view class="operate-container">
			<button type="primary" class="btn" @tap="clickBtn":disabled="!canCheckin">{{btnText}}</button>
			<button type="warn" class="btn" @tap="afresh":disabled="!canCheckin">Retake</button>
		</view>
		<view class="notice-container">
			<text class="notice">Tips</text>
			<text class="desc">When using face recognition attendance system, please take off any hats, sunglasses or masks.</text>
		</view>
	</view>
</template>

<script>
		var QQMapWX=require('../../lib/qqmap-wx-jssdk.min.js');
		var qqmapsdk;
	export default {
		data() {
			return {
				canCheckin:true,
				photoPath:"",
				btnText:"Shot",
				showCamera:true,
				showImage:false
				
			}
		},
		onLoad:function(){
			qqmapsdk=new QQMapWX({
				key:"7TWBZ-IXVWU-RYSVR-2NZKW-27IKE-GVFOB"
			})
		},
		onShow:function(){
			let that = this
			that.ajax(that.url.validCanCheckIn,"GET",null,function(resp){
				let msg = resp.data.msg 
				if(msg!="Valid sign-in time"){
					that.canCheckin = false
					setTimeout(function(){
						uni.showToast({
							title:msg,
							icon:"none"
						},1000)
					})
				}
			})
			
		},
		methods: {
			clickBtn:function(){
				let that=this;
				if(that.btnText=="Shot"){
					let ctx=uni.createCameraContext();
					ctx.takePhoto({
						quality:"high",
						success:function(resp){
							console.log(resp.tempImagePath)
							that.photoPath=resp.tempImagePath
							that.showCamera=false
							that.showImage=true
							that.btnText="Sign-in"
						}
					})
				}else{
					uni.showLoading({
						title:"Signing you in the system. Please wait a moment."
					})
					setTimeout(function(){
						uni.hideLoading()
					},5000)
					
					uni.getLocation({
						type:"wgs84",
						success: function(resp) {
							let latitude = resp.latitude
							let longitude = resp.longitude
							console.log("Location is "+latitude+","+longitude)
							qqmapsdk.reverseGeocoder({
								location:{
									latitude:latitude,
									longitude:longitude
								},
								success:function(resp){
									console.log(resp.result)
									let address=resp.result.address
									uni.uploadFile({
										url:that.url.checkin,
										filePath:that.photoPath,
										name:"photo",
										header:{
											token:uni.getStorageSync("token")
										},
										formData:{
											address:address
										},
										success:function(resp){
											if(resp.statusCode == 500 && resp.data == "不存在人脸模型"){
												uni.hideLoading()
												uni.showModal({
													title: "System Message",
													content:"No face model detected in database for this user. Would you like to create one using this photo?",
													success:function(res){
														if(res.confirm){
															uni.uploadFile({
																url:that.url.createFaceModel,
																filePath:that.photoPath,
																name:"photo",
																header:{
																	token:uni.getStorageSync("token")
																},
																success:function(resp){
																	if(resp.statusCode == 500){
																		uni.showToast({
																			title:resp.data,
																			icon:"none"
																		})
																	}else if(resp.statusCode == 200){
																		uni.showToast({
																			title:"Face model created successfully",
																			icon:"none"
																		})
																	}
																}
															})
														}else if(resp.statusCode == 200){
															let data = JSON.parse(resp.data)
															let code = data.code
															let msg = data.msg
															if(code == 200){
																uni.hideLoading()
																uni.showToast({
																	title:"Signed in successfully",
																	complete:function(){
																		uni.navigateTo({
																			url:"../checkin_result/checkin_result"
																		})
																	}
																})
															}
														}else if(resp.statusCode == 500){
															uni.showToast({
																title:resp.data,
																icon:"none"
															})
														}
														
													}
												})
											}
										}
										
									})
																	
									
									
									
								}
							})							
						}
					})
				}
			},
			afresh:function(){
				let that=this;
				that.showCamera=true;
				that.showImage=false;
				that.btnText="Shot"
			}
		}
	}
</script>

<style lang="less">
	@import url("checkin.less");

</style>
