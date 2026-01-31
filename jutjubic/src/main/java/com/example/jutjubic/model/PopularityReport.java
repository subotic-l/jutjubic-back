package com.example.jutjubic.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "popularity_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PopularityReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime reportDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "first_video_id", nullable = false)
    private VideoPost firstVideo;

    @Column(nullable = false)
    private Double firstVideoScore;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "second_video_id", nullable = false)
    private VideoPost secondVideo;

    @Column(nullable = false)
    private Double secondVideoScore;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "third_video_id", nullable = false)
    private VideoPost thirdVideo;

    @Column(nullable = false)
    private Double thirdVideoScore;
}
