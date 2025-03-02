package company.Leave_Management_service.Services;

import company.Leave_Management_service.DTO.CreateLeaveRequestDTO;
import company.Leave_Management_service.DTO.LeaveRequestDTO;
import company.Leave_Management_service.DTO.UpdateLeaveStatusDTO;
import company.Leave_Management_service.Exeception.InvalidLeaveRequestException;
import company.Leave_Management_service.Models.LeaveRequest;
import company.Leave_Management_service.Repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class LeaveRequestService {
    private final LeaveRequestRepository leaveRequestRepository;

    public LeaveRequestDTO createLeaveRequest(CreateLeaveRequestDTO dto) {
        validateDates(dto.getStartDate(), dto.getEndDate());
        validateLeaveOverlap(dto.getEmployeeId(), dto.getStartDate(), dto.getEndDate());

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setEmployeeId(dto.getEmployeeId());
        leaveRequest.setStartDate(dto.getStartDate());
        leaveRequest.setEndDate(dto.getEndDate());
        leaveRequest.setLeaveType(dto.getLeaveType());
        leaveRequest.setComments(dto.getComments());
        leaveRequest.setStatus(LeaveRequest.LeaveStatus.PENDING);
        leaveRequest.setSubmissionDate(LocalDate.now());

        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        return convertToDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestDTO> getLeaveRequestsByEmployeeId(Long employeeId) {
        if (employeeId == null) {
            throw new InvalidLeaveRequestException("Employee ID cannot be null");
        }
        return leaveRequestRepository.findByEmployeeId(employeeId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public LeaveRequestDTO updateLeaveRequestStatus(Long requestId, UpdateLeaveStatusDTO dto) {
        if (requestId == null || dto == null) {
            throw new InvalidLeaveRequestException("Request ID and update data cannot be null");
        }

        LeaveRequest leaveRequest = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new InvalidLeaveRequestException("Leave request not found with ID: " + requestId));

        validateStatusTransition(leaveRequest.getStatus(), dto.getStatus());

        leaveRequest.setStatus(dto.getStatus());
        leaveRequest.setApproverEmployeeId(dto.getApproverEmployeeId());
        leaveRequest.setApprovalDate(LocalDate.now());

        LeaveRequest updated = leaveRequestRepository.save(leaveRequest);
        return convertToDTO(updated);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestDTO> getAllLeaveRequests() {
        return leaveRequestRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LeaveRequestDTO getLeaveRequestById(Long requestId) {
        if (requestId == null) {
            throw new InvalidLeaveRequestException("Request ID cannot be null");
        }
        return leaveRequestRepository.findById(requestId)
                .map(this::convertToDTO)
                .orElseThrow(() -> new InvalidLeaveRequestException("Leave request not found with ID: " + requestId));
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new InvalidLeaveRequestException("Start date and end date cannot be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new InvalidLeaveRequestException("End date must be after start date");
        }
        if (startDate.isBefore(LocalDate.now())) {
            throw new InvalidLeaveRequestException("Start date cannot be in the past");
        }
    }

    private void validateLeaveOverlap(Long employeeId, LocalDate startDate, LocalDate endDate) {
        List<LeaveRequest> existingRequests = leaveRequestRepository.findByEmployeeId(employeeId);
        boolean hasOverlap = existingRequests.stream()
                .filter(request -> request.getStatus() != LeaveRequest.LeaveStatus.REJECTED
                        && request.getStatus() != LeaveRequest.LeaveStatus.CANCELLED)
                .anyMatch(request ->
                        (startDate.isBefore(request.getEndDate()) || startDate.isEqual(request.getEndDate())) &&
                                (endDate.isAfter(request.getStartDate()) || endDate.isEqual(request.getStartDate()))
                );

        if (hasOverlap) {
            throw new InvalidLeaveRequestException("Leave request overlaps with existing approved or pending leave");
        }
    }

    private void validateStatusTransition(LeaveRequest.LeaveStatus currentStatus, LeaveRequest.LeaveStatus newStatus) {
        if (currentStatus == LeaveRequest.LeaveStatus.CANCELLED
                || currentStatus == LeaveRequest.LeaveStatus.REJECTED) {
            throw new InvalidLeaveRequestException("Cannot update status of a cancelled or rejected leave request");
        }

        if (currentStatus == LeaveRequest.LeaveStatus.APPROVED
                && newStatus != LeaveRequest.LeaveStatus.CANCELLED) {
            throw new InvalidLeaveRequestException("Approved leave requests can only be cancelled");
        }
    }

    private LeaveRequestDTO convertToDTO(LeaveRequest leaveRequest) {
        LeaveRequestDTO dto = new LeaveRequestDTO();
        dto.setRequestId(leaveRequest.getRequestId());
        dto.setEmployeeId(leaveRequest.getEmployeeId());
        dto.setStartDate(leaveRequest.getStartDate());
        dto.setEndDate(leaveRequest.getEndDate());
        dto.setLeaveType(leaveRequest.getLeaveType());
        dto.setStatus(leaveRequest.getStatus());
        dto.setComments(leaveRequest.getComments());
        dto.setApproverEmployeeId(leaveRequest.getApproverEmployeeId());
        dto.setSubmissionDate(leaveRequest.getSubmissionDate());
        dto.setApprovalDate(leaveRequest.getApprovalDate());
        return dto;
    }
}