<template>
	<view class = "page">
		<view class = "summary-container">
			<view class = "user-info">
				<image :src = "photo" mode = "widthFix" class="photo"></image>
				<view class = "info">
					<text class = "username">{{name}}</text>
					<text class = "dept">{{deptName == null?"":deptName}}</text>
				</view>
			</view>
			<view class = "date">{{date}}</view>
		</view>
		<view class="result-container">
			<view class="left">
				<image src="../../static/icon-6.png" mode="widthFix" class="icon-timer"></image>
				<view class="line"></view>
				<image src="../../static/icon-6.png" mode="widthFix" class="icon-timer"></image>
				
			</view>
			<view class="right">
				<view class="row">
					<text class="start">Work Start（{{ attendanceTime }}）</text>
				</view>
				<view class="row">
					<text class="checkin-time">Sign-in time（{{ checkinTime }}）</text>
					<text class="checkin-result green" v-if="status=='Attended'">{{status}}</text>
					<text class="checkin-result yellow" v-if="status=='Late'">{{status}}</text>
				</view>
				<view class="row">
					<image src="../../static/icon-7.png" mode="widthFix" class="icon-small"></image>
					<text class="desc">{{address}}</text>
				</view>
				<view class="row">			
					<text class="desc">Your state new COVID cases today:</text>
					<text class="checkin-result green" v-if="cases<=2000">{{cases}}</text>
					<text class="checkin-result yellow" v-if="cases<=5000 && cases>2000">{{cases}}</text>
					<text class="checkin-result red" v-if="cases>5000">{{cases}}</text>
				</view>
				<view class="row">
					<image src="../../static/icon-8.png" mode="widthFix" class="icon-small"></image>
					<text class="desc">Indentification</text>
					<text class="checkin-result green">Passed</text>
				</view>
				<view class="row">
					<text class="end">Finish（{{ closingTime }}）</text>
				</view>
			</view>
		</view>
		<view class="checkin-report">
			<image src="../../static/big-icon-1.png" mode="widthFix" class="big-icon"></image>
			<view class="report-title">
				<text class="days">{{checkinDays}}</text>
				<text class="unit">days</text>
			</view>
			<view class="sub-title">
				<text>Total Sign-in</text>
				<view class="line"></view>
			</view>
			<view class="calendar-container">
				<view class="calendar" v-for="one in weekCheckin" :key="one">
					<image src="../../static/icon-9.png" mode="widthFix" class="calendar-icon" v-if="one.type=='workday'"></image>
					<image src="../../static/icon-10.png" mode="widthFix" class="calendar-icon" v-if="one.type=='holiday'"></image>
					<text class="day">{{one.day}}</text>
					<text class="result green" v-if="one.status=='Attended'">{{one.status}}</text>
					<text class="result yellow" v-if="one.status=='Late'">{{one.status}}</text>
					<text class="result red" v-if="one.status=='Absent'">{{one.status}}</text>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	export default {
		data() {
			return {
				name:"",
				photo:"",
				deptName:"",
				address:"",
				cases:0,
				checkinTime:"",
				status:"",
				date:"",
				attendanceTime:"",
				closingTime:"",
				checkinDays:0,
				weekCheckin:[]
				
				/*name:"SleepyHead",
				photo:"https://thirdwx.qlogo.cn/mmopen/vi_32/ajNVdqHZLLBFckht4kIvJyictpejiaqBLiaCwQ1cqribYibPCB0vh6uxOH9phupCVKyQwWgiaHOlAzKuKn1eC3mLWIUw/132",
				deptName:"HR",
				address:"Jersey City New Jersey",
				cases:2061,
				checkinTime:"08:45",
				status:"Attended",
				date:"10/12/2021",
				attendanceTime:"9:00",
				closingTime:"17:00",
				checkinDays:201,
				weekCheckin:[
					{type:"workday",day:"Mon",status:"Absent"},
					{type:"workday",day:"Tue",status:"Late"},
					{type:"workday",day:"Wed",status:"Attended"},
					{type:"workday",day:"Thur",status:"Attended"},
					{type:"workday",day:"Fri",status:"Attended"},
					{type:"holiday",day:"Sat",status:""},
					{type:"holiday",day:"Sun",status:""},
				]*/
				
			}
		},
		onShow:function(){
			let that=this
			that.ajax(that.url.searchTodayCheckin,"GET",null,function(resp){
				let result=resp.data.result
				that.name=result.name
				that.photo=result.photo
				that.deptName=result.deptName
				that.address=result.address
				that.cases=result.risk
				that.checkinTime=result.checkinTime
				that.status=result.status
				that.date=result.date
				that.attendanceTime=result.attendanceTime
				that.closingTime=result.closingTime
				that.checkinDays=result.checkinDays
				that.weekCheckin=result.weekCheckin
			})
		},
		methods: {
			
		}
	}
</script>

<style lang="less">
	@import url("checkin_result.less");

</style>
